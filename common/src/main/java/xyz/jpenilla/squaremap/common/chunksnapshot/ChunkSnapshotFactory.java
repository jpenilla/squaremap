package xyz.jpenilla.squaremap.common.chunksnapshot;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import xyz.jpenilla.squaremap.common.Logging;

public final class ChunkSnapshotFactory {
    private ChunkSnapshotFactory() {
    }

    /**
     * Captures a live chunk without retaining mutable block-state or heightmap data.
     * Biome containers are retained because normal section biome updates replace them.
     *
     * <p>Must be called on the owning server thread while the chunk is safe to access.
     * The caller is responsible for completing any required chunk upgrades first.</p>
     *
     * @param level the level containing the chunk
     * @param chunk the chunk to snapshot
     * @return the captured snapshot
     * @throws IllegalStateException if the required world-surface heightmap is missing
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ChunkSnapshot snapshotLiveChunk(final Level level, final ChunkAccess chunk) {
        final LevelHeightAccessor heightAccessor = LevelHeightAccessor.create(chunk.getMinY(), chunk.getHeight());
        final int sectionCount = heightAccessor.getSectionsCount();
        final LevelChunkSection[] sections = chunk.getSections();
        final PalettedContainer<BlockState>[] states = new PalettedContainer[sectionCount];
        final PalettedContainerRO<Holder<Biome>>[] biomes = new PalettedContainerRO[sectionCount];

        final boolean[] empty = new boolean[sectionCount];
        if (!chunk.hasPrimedHeightmap(Heightmap.Types.WORLD_SURFACE)) {
            throw new IllegalStateException("Expected WORLD_SURFACE heightmap to be present, but it wasn't! " + chunk.getPos());
        }
        final Map<Heightmap.Types, HeightmapSnapshot> heightmaps = new EnumMap<>(ChunkSnapshotImpl.EMPTY_HEIGHTMAPS);
        heightmaps.put(Heightmap.Types.WORLD_SURFACE, new HeightmapSnapshot(chunk, heightAccessor, Heightmap.Types.WORLD_SURFACE));

        for (int i = 0; i < sectionCount; i++) {
            final boolean sectionEmpty = sections[i].hasOnlyAir();
            empty[i] = sectionEmpty;

            if (sectionEmpty) {
                states[i] = EmptySectionHolder.getEmptySectionBlockStates();
            } else {
                states[i] = sections[i].getStates().copy();
            }

            // Biome containers are read-only; section updates replace the container.
            biomes[i] = sections[i].getBiomes();
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

    /**
     * Decodes a snapshot directly from saved chunk data without loading a live chunk.
     *
     * <p>The caller must supply independently owned NBT already upgraded to the current
     * data version and classified as {@link ChunkSnapshotEligibility#ELIGIBLE}. This method
     * does not check generation status or complete retrogen and may run off-thread.</p>
     *
     * @param levelHeight the level's vertical bounds
     * @param dimensionType the level's dimension type
     * @param containerFactory the level's palette container factory
     * @param chunkData the upgraded, snapshot-ready chunk data
     * @param x the expected chunk x coordinate
     * @param z the expected chunk z coordinate
     * @return the decoded snapshot
     * @throws IllegalStateException if coordinates do not match or the required
     *     world-surface heightmap is missing
     * @throws SerializableChunkData.ChunkReadException if a section palette cannot be decoded
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ChunkSnapshot snapshotFromChunkData(
        final LevelHeightAccessor levelHeight,
        final DimensionType dimensionType,
        final PalettedContainerFactory containerFactory,
        final CompoundTag chunkData,
        final int x,
        final int z
    ) {
        final ChunkPos chunkPos = new ChunkPos(
            chunkData.getIntOr(SerializableChunkData.X_POS_TAG, 0),
            chunkData.getIntOr(SerializableChunkData.Z_POS_TAG, 0)
        );
        if (chunkPos.x() != x || chunkPos.z() != z) {
            throw new IllegalStateException("Expected chunk at " + new ChunkPos(x, z) + ", but saved data was for " + chunkPos);
        }
        final Map<Heightmap.Types, HeightmapSnapshot> heightmaps = new EnumMap<>(Heightmap.Types.class);
        chunkData.getCompound(SerializableChunkData.HEIGHTMAPS_TAG)
            .flatMap(heightmapsTag -> heightmapsTag.getLongArray(Heightmap.Types.WORLD_SURFACE.getSerializationKey()))
            .ifPresent((longs) -> {
                heightmaps.put(Heightmap.Types.WORLD_SURFACE, new HeightmapSnapshot(longs, levelHeight));
            });
        if (heightmaps.isEmpty()) {
            throw new IllegalStateException("Expected WORLD_SURFACE heightmap to be present, but it wasn't! " + chunkPos);
        }

        final ListTag sectionTags = chunkData.getListOrEmpty(SerializableChunkData.SECTIONS_TAG);
        final int sectionCount = levelHeight.getSectionsCount();

        final PalettedContainer<BlockState>[] states = new PalettedContainer[sectionCount];
        final PalettedContainerRO<Holder<Biome>>[] biomes = new PalettedContainerRO[sectionCount];
        final boolean[] empty = new boolean[sectionCount];
        Arrays.fill(states, EmptySectionHolder.getEmptySectionBlockStates());
        Arrays.fill(biomes, EmptySectionHolder.getEmptySectionBiomes());
        Arrays.fill(empty, true);

        final Codec<PalettedContainerRO<Holder<Biome>>> biomesCodec = containerFactory.biomeContainerCodec();
        final Codec<PalettedContainer<BlockState>> blockStatesCodec = containerFactory.blockStatesContainerCodec();

        for (int i = 0; i < sectionTags.size(); ++i) {
            final Optional<CompoundTag> maybeSectionTag = sectionTags.getCompound(i);
            if (maybeSectionTag.isPresent()) {
                final CompoundTag sectionTag = maybeSectionTag.get();
                final int y = sectionTag.getByteOr(ChunkDataKeys.SECTION_Y, (byte) 0);
                final int index = y - levelHeight.getMinSectionY();
                if (index >= 0 && index < sectionCount) {
                    final Optional<PalettedContainer<BlockState>> maybeBlocks = sectionTag.getCompound(ChunkDataKeys.BLOCK_STATES)
                        .map((container) -> blockStatesCodec.parse(NbtOps.INSTANCE, container).promotePartial((msg) -> Logging.logger().warn("Failed to decode chunk {} section {}: {}", chunkPos, y, msg)).getOrThrow(SerializableChunkData.ChunkReadException::new));
                    final PalettedContainer<BlockState> blocks = maybeBlocks.orElse(EmptySectionHolder.getEmptySectionBlockStates());
                    final Optional<PalettedContainerRO<Holder<Biome>>> maybeBiomes = sectionTag.getCompound(ChunkDataKeys.BIOMES)
                        .map((container) -> biomesCodec.parse(NbtOps.INSTANCE, container).promotePartial((msg) -> Logging.logger().warn("Failed to decode chunk {} section {}: {}", chunkPos, y, msg)).getOrThrow(SerializableChunkData.ChunkReadException::new));
                    final PalettedContainerRO<Holder<Biome>> sectionBiomes = maybeBiomes.orElse(EmptySectionHolder.getEmptySectionBiomes());
                    states[index] = blocks;
                    empty[index] = blocks == EmptySectionHolder.getEmptySectionBlockStates();
                    biomes[index] = sectionBiomes;
                }
            }
        }

        return new ChunkSnapshotImpl(
            levelHeight,
            states,
            biomes,
            heightmaps,
            empty,
            dimensionType,
            chunkPos
        );
    }
}
