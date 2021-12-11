package xyz.jpenilla.squaremap.plugin.util;

import java.util.EnumMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface ChunkSnapshot extends LevelHeightAccessor, BiomeManager.NoiseBiomeSource {
    BlockState getBlockState(BlockPos pos);

    FluidState getFluidState(BlockPos pos);

    int getHeight(Heightmap.Types type, int x, int z);

    DimensionType dimensionType();

    ChunkPos pos();

    boolean sectionEmpty(int sectionIndex);

    static CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(final ServerLevel level, final int x, final int z, final boolean biomesOnly) {
        return level.getChunkSource().getChunkAtAsynchronously(x, z, false, true)
            .thenApply(either -> either.left()
                .map(chunk -> {
                    final LevelChunk levelChunk = (LevelChunk) chunk;
                    if (levelChunk.isEmpty()) {
                        return null;
                    }
                    return ChunkSnapshot.snapshot(levelChunk, biomesOnly);
                })
                .orElse(null));
    }

    @SuppressWarnings("unchecked")
    static ChunkSnapshot snapshot(final LevelChunk chunk, final boolean biomesOnly) {
        // AsyncCatcher.catchOp("Chunk Snapshot");
        final int sectionCount = chunk.getSectionsCount();
        final LevelChunkSection[] sections = chunk.getSections();
        final PalettedContainer<BlockState>[] states = new PalettedContainer[sectionCount];
        final PalettedContainer<Biome>[] biomes = new PalettedContainer[sectionCount];

        final boolean[] empty = new boolean[sectionCount];
        final Heightmap heightmap = new Heightmap(chunk, Heightmap.Types.WORLD_SURFACE);
        if (!biomesOnly) {
            heightmap.setRawData(chunk, Heightmap.Types.WORLD_SURFACE, chunk.heightmaps.get(Heightmap.Types.WORLD_SURFACE).getRawData());
            for (int i = 0; i < sectionCount; i++) {
                final boolean sectionEmpty = sections[i].hasOnlyAir();
                empty[i] = sectionEmpty;

                if (sectionEmpty) {
                    states[i] = ChunkSnapshotImpl.EMPTY_SECTION_BLOCK_STATES;
                } else {
                    states[i] = sections[i].getStates().copy();
                }

                biomes[i] = sections[i].getBiomes().copy();
            }
        } else {
            for (int i = 0; i < sectionCount; i++) {
                biomes[i] = sections[i].getBiomes().copy();
            }
        }

        return new ChunkSnapshotImpl(
            LevelHeightAccessor.create(chunk.getMinBuildHeight(), chunk.getMaxBuildHeight()),
            states,
            biomes,
            Util.make(new EnumMap<>(Heightmap.Types.class), map -> map.put(Heightmap.Types.WORLD_SURFACE, heightmap)),
            empty,
            chunk.level.dimensionType(),
            chunk.getPos()
        );
    }
}
