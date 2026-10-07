package xyz.jpenilla.squaremap.common.render.scanning;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.chunksnapshot.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.util.ColorBlender;
import xyz.jpenilla.squaremap.common.util.Colors;

@DefaultQualifier(NonNull.class)
final class BiomeColorSampler {
    /**
     * Blocks beyond a position whose biome cells {@link BiomeManager#getBiome(int, int, int)} may read.
     */
    static final int ZOOM_MARGIN = 2;
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
    private final Long2ReferenceLinkedOpenHashMap<Biome> biomeCache = new Long2ReferenceLinkedOpenHashMap<>(BLOCKPOS_BIOME_CACHE_SIZE);
    private final BiomeColorTables colorData;
    private final BiomeManager biomeManager;
    private final BiomeResolver fallbackBiomes;
    private final SnapshotLookup snapshots;
    private final int blend;

    BiomeColorSampler(
        final BiomeColorTables colorData,
        final BiomeManager biomeManager,
        final BiomeResolver fallbackBiomes,
        final int blend,
        final SnapshotLookup snapshots
    ) {
        this.blend = blend;
        this.colorData = colorData;
        this.biomeManager = biomeManager.withDifferentSource(this::noiseBiome);
        this.fallbackBiomes = fallbackBiomes;
        this.snapshots = snapshots;
    }

    int modifyColorFromBiome(int color, final ChunkSnapshot chunk, final BlockPos pos) {
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
        if (this.blend > 0) {
            final @Nullable Biome uniform = this.uniformBlendBiome(pos);
            // Swamp grass color varies by position, so its samples must still be blended.
            if (uniform != null && uniform.getSpecialEffects().grassColorModifier() != BiomeSpecialEffects.GrassColorModifier.SWAMP) {
                return this.grassColorSampler(uniform, pos);
            }
            return this.sampleNeighbors(pos, this.blend, this::grassColorSampler);
        }
        return this.grassColorSampler(this.biome(pos), pos);
    }

    private int grassColorSampler(final Biome biome, final BlockPos pos) {
        return biome.getSpecialEffects().grassColorModifier().modifyColor(pos.getX(), pos.getZ(), this.colorData.grassColors().getInt(biome));
    }

    private int foliage(final BlockPos pos) {
        if (this.blend > 0) {
            final @Nullable Biome uniform = this.uniformBlendBiome(pos);
            if (uniform != null) {
                return this.colorData.foliageColors().getInt(uniform);
            }
            return this.sampleNeighbors(pos, this.blend, (biome, b) -> this.colorData.foliageColors().getInt(biome));
        }
        return this.colorData.foliageColors().getInt(this.biome(pos));
    }

    private int water(final BlockPos pos) {
        if (this.blend > 0) {
            final @Nullable Biome uniform = this.uniformBlendBiome(pos);
            if (uniform != null) {
                return this.colorData.waterColors().getInt(uniform);
            }
            return this.sampleNeighbors(pos, this.blend, (biome, b) -> this.colorData.waterColors().getInt(biome));
        }
        return this.colorData.waterColors().getInt(this.biome(pos));
    }

    @FunctionalInterface
    interface ColorSampler {
        int sample(Biome biome, BlockPos pos);
    }

    @FunctionalInterface
    interface SnapshotLookup {
        @Nullable ChunkSnapshot get(int chunkX, int chunkZ);
    }

    private int sampleNeighbors(final BlockPos pos, final int radius, final ColorSampler colorSampler) {
        this.colorBlender.reset();

        // Sampling in the y direction as well would improve output, however would slow down rendering significantly (biomes already dominate)
        for (int x = pos.getX() - radius; x < pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z < pos.getZ() + radius; z++) {
                this.mutablePos.set(x, pos.getY(), z);

                this.colorBlender.addColor(colorSampler.sample(this.biome(this.mutablePos), this.mutablePos));
            }
        }

        return this.colorBlender.result();
    }

    /**
     * Returns the biome every blend sample around a position resolves to, without per-sample lookups.
     *
     * @param pos the blended position
     * @return the biome, or {@code null} if samples may resolve to different biomes
     */
    private @Nullable Biome uniformBlendBiome(final BlockPos pos) {
        // Samples cover [pos - blend, pos + blend) at pos's Y, and each zoomed lookup reads
        // cells up to ZOOM_MARGIN blocks further on every axis.
        final int reach = this.blend + ZOOM_MARGIN;
        @Nullable Holder<Biome> uniform = null;
        for (int chunkX = (pos.getX() - reach) >> 4; chunkX <= (pos.getX() + reach - 1) >> 4; chunkX++) {
            for (int chunkZ = (pos.getZ() - reach) >> 4; chunkZ <= (pos.getZ() + reach - 1) >> 4; chunkZ++) {
                final @Nullable ChunkSnapshot chunk = this.snapshots.get(chunkX, chunkZ);
                if (chunk == null) {
                    return null;
                }
                final @Nullable Holder<Biome> below = chunk.uniformBiome(pos.getY() - ZOOM_MARGIN);
                if (below == null || below != chunk.uniformBiome(pos.getY() + ZOOM_MARGIN) || uniform != null && uniform != below) {
                    return null;
                }
                uniform = below;
            }
        }
        return uniform == null ? null : uniform.value();
    }

    private Biome biome(final BlockPos pos) {
        final long blockKey = pos.asLong();
        final @Nullable Biome cached = this.biomeCache.get(blockKey);
        if (cached != null) {
            return cached;
        }

        final Biome biome = this.biomeManager.getBiome(pos).value();

        if (this.biomeCache.size() >= BLOCKPOS_BIOME_CACHE_SIZE) {
            this.biomeCache.removeLast();
        }
        this.biomeCache.putAndMoveToFirst(blockKey, biome);
        return biome;
    }

    private Holder<Biome> noiseBiome(final int quartX, final int quartY, final int quartZ) {
        final @Nullable ChunkSnapshot chunk = this.snapshots.get(QuartPos.toSection(quartX), QuartPos.toSection(quartZ));
        final BiomeResolver noiseBiomeSource = chunk == null
            ? this.fallbackBiomes // no eligible chunk exists, this will get from the chunk generator
            : chunk;
        return noiseBiomeSource.getNoiseBiome(quartX, quartY, quartZ);
    }
}
