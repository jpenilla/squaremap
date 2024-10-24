package xyz.jpenilla.squaremap.common.data;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.task.render.AbstractRender;
import xyz.jpenilla.squaremap.common.util.ColorBlender;
import xyz.jpenilla.squaremap.common.util.Colors;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshot;

@DefaultQualifier(NonNull.class)
public final class BiomeColors {
    private static final int BLOCKPOS_BIOME_CACHE_SIZE = 4096;

    private static final Set<Block> GRASS_COLOR_BLOCKS = Set.of(
        Blocks.GRASS_BLOCK,
        Blocks.SHORT_GRASS,
        Blocks.TALL_GRASS,
        Blocks.FERN,
        Blocks.LARGE_FERN,
        Blocks.POTTED_FERN,
        Blocks.SUGAR_CANE
    );

    private static final Set<Block> FOLIAGE_COLOR_BLOCKS = Set.of(
        Blocks.VINE,
        Blocks.OAK_LEAVES,
        Blocks.JUNGLE_LEAVES,
        Blocks.ACACIA_LEAVES,
        Blocks.DARK_OAK_LEAVES,
        Blocks.MANGROVE_LEAVES
    );

    private final ColorBlender colorBlender = new ColorBlender();
    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
    private final LevelBiomeColorData colorData;
    private final MapWorldInternal world;
    private final BiomeCache biomeCache;

    public BiomeColors(final MapWorldInternal world, final AbstractRender.ChunkSnapshotManager chunkSnapshotProvider) {
        this.world = world;
        this.colorData = world.levelBiomeColorData();
        this.biomeCache = BiomeCache.sized(this.world.serverLevel(), chunkSnapshotProvider, BLOCKPOS_BIOME_CACHE_SIZE);
    }

    public int modifyColorFromBiome(int color, final ChunkSnapshot chunk, final BlockPos pos) {
        final BlockState data = chunk.getBlockState(pos);
        final Block block = data.getBlock();

        if (GRASS_COLOR_BLOCKS.contains(block)) {
            color = this.grass(pos);
        } else if (FOLIAGE_COLOR_BLOCKS.contains(block)) {
            color = this.foliage(pos);
        } else if (block.defaultMapColor() == MapColor.WATER) {
            int modColor = this.water(pos);
            color = Colors.mix(color, modColor, 0.8F);
        }

        return color;
    }

    private int grass(final BlockPos pos) {
        if (this.world.config().MAP_BIOMES_BLEND > 0) {
            return this.sampleNeighbors(pos, this.world.config().MAP_BIOMES_BLEND, this::grassColorSampler);
        }
        return this.grassColorSampler(this.biome(pos), pos);
    }

    private int grassColorSampler(final Biome biome, final BlockPos pos) {
        return biome.getSpecialEffects().getGrassColorModifier().modifyColor(pos.getX(), pos.getZ(), this.colorData.grassColors().getInt(biome));
    }

    private int foliage(final BlockPos pos) {
        if (this.world.config().MAP_BIOMES_BLEND > 0) {
            return this.sampleNeighbors(pos, this.world.config().MAP_BIOMES_BLEND, (biome, b) -> this.colorData.foliageColors().getInt(biome));
        }
        return this.colorData.foliageColors().getInt(this.biome(pos));
    }

    private int water(final BlockPos pos) {
        if (this.world.config().MAP_BIOMES_BLEND > 0) {
            return this.sampleNeighbors(pos, this.world.config().MAP_BIOMES_BLEND, (biome, b) -> this.colorData.waterColors().getInt(biome));
        }
        return this.colorData.waterColors().getInt(this.biome(pos));
    }

    @FunctionalInterface
    interface ColorSampler {
        int sample(Biome biome, BlockPos pos);
    }

    private int sampleNeighbors(final BlockPos pos, final int radius, final ColorSampler colorSampler) {
        this.colorBlender.reset();

        // Sampling in the y direction as well would improve output, however would complicate caching (low priority, PRs accepted)
        for (int x = pos.getX() - radius; x < pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z < pos.getZ() + radius; z++) {
                this.mutablePos.set(x, pos.getY(), z);

                this.colorBlender.addColor(colorSampler.sample(this.biome(this.mutablePos), this.mutablePos));
            }
        }

        return this.colorBlender.result();
    }

    private Biome biome(final BlockPos pos) {
        return this.biomeCache.biome(pos);
    }

    private static final class BiomeCache {
        private final ServerLevel level;
        private final AbstractRender.ChunkSnapshotManager chunkSnapshotManager;
        private final int size;
        private final Long2ReferenceLinkedOpenHashMap<Biome> cache;
        private final BiomeManager biomeManager;

        private BiomeCache(
            final ServerLevel level,
            final AbstractRender.ChunkSnapshotManager chunkSnapshotManager,
            final int size
        ) {
            this.level = level;
            this.chunkSnapshotManager = chunkSnapshotManager;
            this.size = size;
            this.cache = new Long2ReferenceLinkedOpenHashMap<>(size);
            this.biomeManager = this.level.getBiomeManager().withDifferentSource(this::noiseBiome);
        }

        public Biome biome(final BlockPos pos) {
            final long blockKey = pos.asLong();
            final @Nullable Biome cached = this.cache.get(blockKey);
            if (cached != null) {
                return cached;
            }

            final Biome biome = this.biomeManager.getBiome(pos).value();

            if (this.cache.size() >= this.size) {
                this.cache.removeLast();
            }
            this.cache.putAndMoveToFirst(blockKey, biome);
            return biome;
        }

        private Holder<Biome> noiseBiome(final int quartX, final int quartY, final int quartZ) {
            final ChunkPos chunkPos = new ChunkPos(
                QuartPos.toSection(quartX),
                QuartPos.toSection(quartZ)
            );
            final @Nullable ChunkSnapshot chunk = this.chunkSnapshotManager.snapshotDirect(chunkPos).join();

            final BiomeManager.NoiseBiomeSource noiseBiomeSource = chunk == null
                ? this.level::getUncachedNoiseBiome // no chunk exists, this will get from the chunk generator
                : chunk;

            return noiseBiomeSource.getNoiseBiome(quartX, quartY, quartZ);
        }

        public static BiomeCache sized(final ServerLevel level, final AbstractRender.ChunkSnapshotManager snapshotCache, final int size) {
            return new BiomeCache(level, snapshotCache, size);
        }
    }
}
