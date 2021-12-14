package xyz.jpenilla.squaremap.plugin.util;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
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
record ChunkSnapshotImpl(
    LevelHeightAccessor heightAccessor,
    PalettedContainer<BlockState>[] states,
    PalettedContainer<Biome>[] biomes,
    Map<Heightmap.Types, Heightmap> heightmaps,
    boolean[] emptySections,
    DimensionType dimensionType,
    ChunkPos pos
) implements ChunkSnapshot {
    @SuppressWarnings("deprecation")
    static final PalettedContainer<BlockState> EMPTY_SECTION_BLOCK_STATES = new PalettedContainer<>(
        Block.BLOCK_STATE_REGISTRY,
        Blocks.AIR.defaultBlockState(),
        PalettedContainer.Strategy.SECTION_STATES
    );

    @Override
    public BlockState getBlockState(final BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    private BlockState getBlockState(final int x, final int y, final int z) {
        final int sectionIndex = this.getSectionIndex(y);
        if (sectionIndex < 0 || sectionIndex >= this.states.length || this.sectionEmpty(sectionIndex)) {
            return Blocks.AIR.defaultBlockState();
        }
        return this.states[sectionIndex].get((y & 15) << 8 | (z & 15) << 4 | x & 15);
    }

    @Override
    public FluidState getFluidState(final BlockPos pos) {
        return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
    }

    private FluidState getFluidState(final int x, final int y, final int z) {
        final int sectionIndex = this.getSectionIndex(y);
        if (sectionIndex < 0 || sectionIndex >= this.states.length || this.sectionEmpty(sectionIndex)) {
            return Fluids.EMPTY.defaultFluidState();
        }
        return this.states[sectionIndex].get((y & 15) << 8 | (z & 15) << 4 | x & 15).getFluidState();
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
    public int getHeight() {
        return this.heightAccessor.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return this.heightAccessor.getMinBuildHeight();
    }

    @Override
    public boolean sectionEmpty(final int sectionIndex) {
        return this.emptySections[sectionIndex];
    }

    @Override
    public Biome getNoiseBiome(final int quartX, final int quartY, final int quartZ) {
        final int minQuartY = QuartPos.fromBlock(this.getMinBuildHeight());
        final int maxQuartY = minQuartY + QuartPos.fromBlock(this.getHeight()) - 1;
        final int clampedQuartY = Mth.clamp(quartY, minQuartY, maxQuartY);
        final int sectionIndex = this.getSectionIndex(QuartPos.toBlock(clampedQuartY));
        return this.biomes[sectionIndex].get(quartX & 3, clampedQuartY & 3, quartZ & 3);
    }
}
