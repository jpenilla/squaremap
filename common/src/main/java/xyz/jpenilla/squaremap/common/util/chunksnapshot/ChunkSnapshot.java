package xyz.jpenilla.squaremap.common.util.chunksnapshot;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface ChunkSnapshot extends LevelHeightAccessor, BiomeManager.NoiseBiomeSource {
    BlockState getBlockState(BlockPos pos);

    FluidState getFluidState(BlockPos pos);

    int getHeight(Heightmap.Types type, int x, int z);

    DimensionType dimensionType();

    ChunkPos pos();

    boolean sectionEmpty(int sectionIndex);

    @SuppressWarnings({"unchecked", "rawtypes"})
    static ChunkSnapshot snapshot(final Level level, final ChunkAccess chunk, final boolean biomesOnly) {
        final LevelHeightAccessor heightAccessor = LevelHeightAccessor.create(chunk.getMinY(), chunk.getHeight());
        final int sectionCount = heightAccessor.getSectionsCount();
        final LevelChunkSection[] sections = chunk.getSections();
        final PalettedContainer<BlockState>[] states = new PalettedContainer[sectionCount];
        final PalettedContainer<Holder<Biome>>[] biomes = new PalettedContainer[sectionCount];

        final boolean[] empty = new boolean[sectionCount];
        Map<Heightmap.Types, HeightmapSnapshot> heightmaps = ChunkSnapshotImpl.EMPTY_HEIGHTMAPS;
        if (!biomesOnly) {
            if (!chunk.hasPrimedHeightmap(Heightmap.Types.WORLD_SURFACE)) {
                if (chunk.getBelowZeroRetrogen() == null) {
                    throw new IllegalStateException("Expected WORLD_SURFACE heightmap to be present, but it wasn't! " + chunk.getPos());
                } else {
                    Heightmap.primeHeightmaps(chunk, Set.of(Heightmap.Types.WORLD_SURFACE));
                }
            }
            heightmaps = new EnumMap<>(ChunkSnapshotImpl.EMPTY_HEIGHTMAPS);
            heightmaps.put(Heightmap.Types.WORLD_SURFACE, new HeightmapSnapshot(chunk, heightAccessor, Heightmap.Types.WORLD_SURFACE));

            for (int i = 0; i < sectionCount; i++) {
                final boolean sectionEmpty = sections[i].hasOnlyAir();
                empty[i] = sectionEmpty;

                if (sectionEmpty) {
                    states[i] = EmptySectionHolder.getEmptySectionBlockStates();
                } else {
                    states[i] = sections[i].getStates().copy();
                }

                biomes[i] = ((PalettedContainer) sections[i].getBiomes()).copy();
            }
        } else {
            for (int i = 0; i < sectionCount; i++) {
                biomes[i] = ((PalettedContainer) sections[i].getBiomes()).copy();
            }
        }

        return new ChunkSnapshotImpl(
            heightAccessor,
            states,
            biomes,
            heightmaps,
            empty,
            level.dimensionType(),
            chunk.getPos()
        );
    }
}
