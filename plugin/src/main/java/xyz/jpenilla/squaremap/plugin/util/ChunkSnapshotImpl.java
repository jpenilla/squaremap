package xyz.jpenilla.squaremap.plugin.util;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
final class ChunkSnapshotImpl implements ChunkSnapshot {
    private final LevelHeightAccessor heightAccessor;
    private final PalettedContainer<BlockState>[] states;
    private final PalettedContainer<Biome>[] biomes;
    private final Map<Heightmap.Types, Heightmap> heightmaps;
    private final boolean[] emptySections;
    private final DimensionType dimensionType;
    private final BiomeManager biomeManager;
    private final ChunkPos pos;

    public ChunkSnapshotImpl(
        final LevelHeightAccessor heightAccessor,
        final PalettedContainer<BlockState>[] states,
        final PalettedContainer<Biome>[] biomes,
        final Map<Heightmap.Types, Heightmap> heightmaps,
        final boolean[] emptySections,
        final DimensionType dimensionType,
        final long seed,
        final ChunkPos pos
    ) {
        this.heightAccessor = heightAccessor;
        this.states = states;
        this.biomes = biomes;
        this.heightmaps = heightmaps;
        this.emptySections = emptySections;
        this.dimensionType = dimensionType;
        this.pos = pos;
        this.biomeManager = new BiomeManager(this, seed);
    }

    @Override
    public BlockState getBlockState(final BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    private BlockState getBlockState(final int x, final int y, final int z) {
        final int i = this.getSectionIndex(y);
        if (this.sectionEmpty(i)) {
            return Blocks.AIR.defaultBlockState();
        }
        return this.states[i].get((y & 15) << 8 | (z & 15) << 4 | x & 15);
    }

    @Override
    public FluidState getFluidState(final BlockPos pos) {
        return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
    }

    private FluidState getFluidState(int x, int y, int z) {
        final int i = this.getSectionIndex(y);
        if (this.sectionEmpty(i)) {
            return Fluids.EMPTY.defaultFluidState();
        }
        return this.states[i].get((y & 15) << 8 | (z & 15) << 4 | x & 15).getFluidState();
    }

    @Override
    public int getHeight(final Heightmap.Types type, final int x, final int z) {
        final Heightmap heightmap = this.heightmaps.get(type);
        if (heightmap == null) {
            throw new RuntimeException("Missing heightmaps " + type);
        }
        return heightmap.getFirstAvailable(x & 15, z & 15) - 1;
    }

    @Override
    public Biome getBiome(final BlockPos pos) {
        return this.biomeManager.getBiome(pos);
    }

    @Override
    public int getHeight() {
        return this.heightAccessor.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return this.heightAccessor.getMinBuildHeight();
    }

    @Override
    public ChunkPos pos() {
        return this.pos;
    }

    @Override
    public boolean sectionEmpty(final int sectionIndex) {
        return this.emptySections[sectionIndex];
    }

    @Override
    public Biome getNoiseBiome(final int biomeX, final int biomeY, final int biomeZ) {
        int l = QuartPos.fromBlock(this.getMinBuildHeight());
        int i1 = l + QuartPos.fromBlock(this.getHeight()) - 1;
        int j1 = Mth.clamp(biomeY, l, i1);
        int sectionIndex = this.getSectionIndex(QuartPos.toBlock(j1));
        return this.biomes[sectionIndex].get(biomeX & 3, j1 & 3, biomeZ & 3);
    }

    @Override
    public DimensionType dimensionType() {
        return dimensionType;
    }
}
