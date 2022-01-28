package xyz.jpenilla.squaremap.common.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.util.Colors;

@DefaultQualifier(NonNull.class)
public final class BiomeColors {
    private static final int CHUNK_SNAPSHOT_CACHE_SIZE = 64;
    private static final int BLOCKPOS_BIOME_CACHE_SIZE = 4096;

    private static final Set<Block> GRASS_COLOR_BLOCKS = Set.of(
        Blocks.GRASS_BLOCK,
        Blocks.GRASS,
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
        Blocks.DARK_OAK_LEAVES
    );

    private static final Set<Block> WATER_COLOR_BLOCKS = Set.of(
        Blocks.WATER,
        Blocks.BUBBLE_COLUMN,
        Blocks.WATER_CAULDRON
    );

    private static final Set<Material> WATER_COLOR_MATERIALS = Set.of(
        Material.WATER_PLANT,
        Material.REPLACEABLE_WATER_PLANT
    );

    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
    private final LevelBiomeColorData colorData;
    private final MapWorldInternal world;
    private final ChunkSnapshotCache chunkSnapshotCache;
    private final BiomeCache biomeCache;

    public BiomeColors(final MapWorldInternal world) {
        this.world = world;
        this.colorData = world.levelBiomeColorData();
        this.chunkSnapshotCache = ChunkSnapshotCache.sized(this.world.serverLevel(), CHUNK_SNAPSHOT_CACHE_SIZE);
        this.biomeCache = BiomeCache.sized(this.world.serverLevel(), this.chunkSnapshotCache, BLOCKPOS_BIOME_CACHE_SIZE);
    }

    public int modifyColorFromBiome(int color, final ChunkSnapshot chunk, final BlockPos pos) {
        this.chunkSnapshotCache.put(chunk);

        final BlockState data = chunk.getBlockState(pos);
        final Material mat = data.getMaterial();
        final Block block = data.getBlock();

        if (GRASS_COLOR_BLOCKS.contains(block)) {
            color = this.grass(pos);
        } else if (FOLIAGE_COLOR_BLOCKS.contains(block)) {
            color = this.foliage(pos);
        } else if (WATER_COLOR_BLOCKS.contains(block) || WATER_COLOR_MATERIALS.contains(mat)) {
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
        return this.modifiedGrassColor(biome, pos, this.colorData.grassColors().getInt(biome));
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
        int rgb;
        int r = 0;
        int g = 0;
        int b = 0;
        int count = 0;
        // Sampling in the y direction as well would improve output, however would complicate caching (low priority, PRs accepted)
        for (int x = pos.getX() - radius; x < pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z < pos.getZ() + radius; z++) {
                this.mutablePos.set(x, pos.getY(), z);
                final Biome biome = this.biome(this.mutablePos);
                rgb = colorSampler.sample(biome, this.mutablePos);
                r += (rgb >> 16) & 0xFF;
                g += (rgb >> 8) & 0xFF;
                b += rgb & 0xFF;
                count++;
            }
        }
        rgb = r / count;
        rgb = (rgb << 8) + g / count;
        rgb = (rgb << 8) + b / count;
        return rgb;
    }

    private Biome biome(final BlockPos pos) {
        return this.biomeCache.biome(pos);
    }

    private int modifiedGrassColor(final Biome biome, final BlockPos pos, final int color) {
        return switch (biome.getSpecialEffects().getGrassColorModifier()) {
            case NONE -> color;
            case SWAMP -> modifiedSwampGrassColor(pos);
            case DARK_FOREST -> (color & 0xFEFEFE) + 2634762 >> 1;
        };
    }

    private static int modifiedSwampGrassColor(final BlockPos pos) {
        // swamps have 2 grass colors, depends on sample from noise generator
        final double sample = Biome.BIOME_INFO_NOISE.getValue(
            pos.getX() * 0.0225,
            pos.getZ() * 0.0225,
            false
        );
        if (sample < -0.1) {
            return 5011004;
        }
        return 6975545;
    }

    private static final class BiomeCache {
        private final ServerLevel level;
        private final ChunkSnapshotCache chunkSnapshotCache;
        private final int size;
        private final Long2ReferenceLinkedOpenHashMap<Biome> cache;
        private final BiomeManager biomeManager;

        private BiomeCache(
            final ServerLevel level,
            final ChunkSnapshotCache chunkSnapshotCache,
            final int size
        ) {
            this.level = level;
            this.chunkSnapshotCache = chunkSnapshotCache;
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

            final Biome biome = this.biomeManager.getBiome(pos);

            if (this.cache.size() >= this.size) {
                this.cache.removeLast();
            }
            this.cache.putAndMoveToFirst(blockKey, biome);
            return biome;
        }

        private Biome noiseBiome(final int quartX, final int quartY, final int quartZ) {
            final ChunkPos chunkPos = new ChunkPos(
                QuartPos.toSection(quartX),
                QuartPos.toSection(quartZ)
            );
            final @Nullable ChunkSnapshot chunk = this.chunkSnapshotCache.snapshot(chunkPos);

            final BiomeManager.NoiseBiomeSource noiseBiomeSource = chunk == null
                ? this.level::getUncachedNoiseBiome // no chunk exists, this will get from the chunk generator
                : chunk;

            return noiseBiomeSource.getNoiseBiome(quartX, quartY, quartZ);
        }

        public static BiomeCache sized(final ServerLevel level, final ChunkSnapshotCache snapshotCache, final int size) {
            return new BiomeCache(level, snapshotCache, size);
        }
    }

    private record ChunkSnapshotCache(
        ServerLevel level,
        int size,
        Long2ObjectLinkedOpenHashMap<ChunkSnapshot> cache
    ) {
        public void put(final ChunkSnapshot snapshot) {
            if (this.cache.size() >= this.size()) {
                this.cache.removeLast();
            }
            this.cache.putAndMoveToFirst(snapshot.pos().toLong(), snapshot);
        }

        public @Nullable ChunkSnapshot snapshot(final ChunkPos chunkPos) {
            final long chunkKey = chunkPos.toLong();

            final @Nullable ChunkSnapshot cached = this.cache.getAndMoveToFirst(chunkKey);
            if (cached != null) {
                return cached;
            }

            @Nullable final ChunkSnapshot chunk = SquaremapCommon.instance().platform().chunkSnapshotProvider()
                .asyncSnapshot(this.level(), chunkPos.x, chunkPos.z, true)
                // todo respect cancellation
                .join();
            if (chunk == null) {
                return null;
            }

            if (this.cache.size() >= this.size()) {
                this.cache.removeLast();
            }
            this.cache.putAndMoveToFirst(chunkKey, chunk);
            return chunk;
        }

        public static ChunkSnapshotCache sized(final ServerLevel level, final int size) {
            return new ChunkSnapshotCache(
                level,
                size,
                new Long2ObjectLinkedOpenHashMap<>(size)
            );
        }
    }
}
