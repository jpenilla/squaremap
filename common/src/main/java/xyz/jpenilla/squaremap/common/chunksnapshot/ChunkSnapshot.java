package xyz.jpenilla.squaremap.common.chunksnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface ChunkSnapshot extends LevelHeightAccessor, BiomeResolver {
    BlockState getBlockState(BlockPos pos);

    FluidState getFluidState(BlockPos pos);

    int getHeight(Heightmap.Types type, int x, int z);

    DimensionType dimensionType();

    ChunkPos pos();

    boolean sectionEmpty(int sectionIndex);

    /**
     * Returns the only biome in the section containing a block Y, clamped to the chunk's height
     * like biome lookups.
     *
     * @param y the block Y
     * @return the section's biome, or {@code null} if the section may contain more than one
     */
    @Nullable Holder<Biome> uniformBiome(int y);
}
