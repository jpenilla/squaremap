package xyz.jpenilla.squaremap.common.chunksnapshot;

import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkSnapshotFactoryTest {
    private static PalettedContainerFactory factory;
    private static DimensionType dimensionType;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        final var registries = VanillaRegistries.createWorldLookup();
        final MappedRegistry<Biome> biomes = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        biomes.register(Biomes.PLAINS, registries.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS).value(), RegistrationInfo.BUILT_IN);
        biomes.freeze();
        factory = PalettedContainerFactory.create(new RegistryAccess.ImmutableRegistryAccess(List.of(biomes)));
        dimensionType = registries.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD).value();
        EmptySectionHolder.init(factory);
    }

    @ParameterizedTest
    @CsvSource({"-64, 384, false", "-64, 384, true", "0, 256, false", "0, 256, true"})
    void reconstructsWorldSurfaceLikeVanilla(final int minY, final int height, final boolean wrongLength) {
        final ProtoChunk chunk = terrainFixture(minY, height);
        final CompoundTag saved = serialize(chunk);
        if (wrongLength) {
            final CompoundTag heightmaps = saved.getCompound(SerializableChunkData.HEIGHTMAPS_TAG).orElseThrow();
            final String key = Heightmap.Types.WORLD_SURFACE.getSerializationKey();
            final long[] data = heightmaps.getLongArray(key).orElseThrow();
            heightmaps.putLongArray(key, Arrays.copyOf(data, data.length - 1));
        } else {
            saved.remove(SerializableChunkData.HEIGHTMAPS_TAG);
        }

        final ChunkSnapshot snapshot = ChunkSnapshotFactory.snapshotFromChunkData(chunk, dimensionType, factory, saved);
        final int[] expected = new int[256];
        final int[] actual = new int[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                expected[z * 16 + x] = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                actual[z * 16 + x] = snapshot.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            }
        }
        assertArrayEquals(expected, actual);
    }

    @Test
    void identifiesEmptyDecodedSections() {
        final ProtoChunk chunk = terrainFixture(-64, 384);
        final ChunkSnapshot snapshot = ChunkSnapshotFactory.snapshotFromChunkData(chunk, dimensionType, factory, serialize(chunk));
        for (int i = 0; i < chunk.getSectionsCount(); i++) {
            assertEquals(chunk.getSection(i).hasOnlyAir(), snapshot.sectionEmpty(i), "Section " + i);
        }
    }

    private static ProtoChunk terrainFixture(final int minY, final int height) {
        final ProtoChunk chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, LevelHeightAccessor.create(minY, height), factory, null);
        chunk.setBlockState(new BlockPos(1, minY, 2), Blocks.STONE.defaultBlockState(), 0);
        chunk.setBlockState(new BlockPos(3, minY + height - 1, 4), Blocks.STONE.defaultBlockState(), 0);
        chunk.setBlockState(new BlockPos(5, minY + 15, 6), Blocks.STONE.defaultBlockState(), 0);
        chunk.setBlockState(new BlockPos(5, minY + 16, 6), Blocks.STONE.defaultBlockState(), 0);
        chunk.setBlockState(new BlockPos(7, minY + 32, 8), Blocks.WATER.defaultBlockState(), 0);
        Heightmap.primeHeightmaps(chunk, Set.of(Heightmap.Types.WORLD_SURFACE));
        return chunk;
    }

    private static CompoundTag serialize(final ProtoChunk chunk) {
        // Serialize all sections, including all-air ones, through vanilla's writer.
        return new SerializableChunkData(
            factory, chunk.getPos(), chunk.getMinSectionY(), 0L, 0L, ChunkStatus.FULL,
            null, null, UpgradeData.EMPTY,
            Map.of(Heightmap.Types.WORLD_SURFACE, chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE).getRawData().clone()),
            new ChunkAccess.PackedTicks(List.of(), List.of()), new ShortList[chunk.getSectionsCount()], false,
            IntStream.range(0, chunk.getSectionsCount())
                .mapToObj(i -> new SerializableChunkData.SectionData(chunk.getMinSectionY() + i, chunk.getSection(i), null, null))
                .toList(),
            List.of(), List.of(), new CompoundTag()
        ).write();
    }
}
