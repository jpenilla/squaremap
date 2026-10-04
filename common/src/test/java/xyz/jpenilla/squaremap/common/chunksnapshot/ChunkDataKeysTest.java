package xyz.jpenilla.squaremap.common.chunksnapshot;

import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ChunkDataKeysTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void emptySectionUsesExpectedKeys() {
        final SerializableChunkData chunk = emptyChunkData(null);
        final CompoundTag data = chunk.write();
        assertEquals(chunk.chunkPos().x(), data.getInt(SerializableChunkData.X_POS_TAG).orElseThrow());
        assertEquals(chunk.chunkPos().z(), data.getInt(SerializableChunkData.Z_POS_TAG).orElseThrow());
        assertEquals(chunk.chunkStatus(), data.read(ChunkDataKeys.STATUS, ChunkStatus.CODEC).orElseThrow());
        final CompoundTag heightmaps = data.getCompound(SerializableChunkData.HEIGHTMAPS_TAG).orElseThrow();
        assertArrayEquals(chunk.heightmaps().get(Heightmap.Types.WORLD_SURFACE),
            heightmaps.getLongArray(Heightmap.Types.WORLD_SURFACE.getSerializationKey()).orElseThrow());
        final ListTag sections = data.getList(SerializableChunkData.SECTIONS_TAG).orElseThrow();
        assertEquals(1, sections.size());
        final CompoundTag section = sections.getCompound(0).orElseThrow();
        assertEquals((byte) -4, section.getByte(ChunkDataKeys.SECTION_Y).orElseThrow());
        assertEquals(chunk.containerFactory().defaultBlockState(), chunk.containerFactory().blockStatesContainerCodec()
            .parse(NbtOps.INSTANCE, section.getCompound(ChunkDataKeys.BLOCK_STATES).orElseThrow())
            .getOrThrow().get(0, 0, 0));
        assertEquals(chunk.containerFactory().defaultBiome(), chunk.containerFactory().biomeContainerCodec()
            .parse(NbtOps.INSTANCE, section.getCompound(ChunkDataKeys.BIOMES).orElseThrow())
            .getOrThrow().get(0, 0, 0));
        assertFalse(data.contains(ChunkDataKeys.RETROGEN));
    }

    @Test
    void pendingRetrogenUsesExpectedKey() {
        // Build through the codec because BelowZeroRetrogen has no public constructor.
        final CompoundTag input = new CompoundTag();
        input.store("target_status", ChunkStatus.CODEC, ChunkStatus.FULL);
        final BelowZeroRetrogen pending = BelowZeroRetrogen.CODEC.parse(NbtOps.INSTANCE, input).getOrThrow();
        final CompoundTag data = emptyChunkData(pending).write();
        final BelowZeroRetrogen decoded = data.read(ChunkDataKeys.RETROGEN, BelowZeroRetrogen.CODEC).orElseThrow();
        assertEquals(ChunkStatus.FULL, decoded.targetStatus());
    }

    private static SerializableChunkData emptyChunkData(final @Nullable BelowZeroRetrogen retroGen) {
        final MappedRegistry<Biome> biomes = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        biomes.register(Biomes.PLAINS, new Biome.BiomeBuilder()
            .temperature(0.8F)
            .downfall(0.4F)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(0).build())
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .build(), RegistrationInfo.BUILT_IN);
        biomes.freeze();
        final PalettedContainerFactory factory = PalettedContainerFactory.create(new RegistryAccess.ImmutableRegistryAccess(List.of(biomes)));
        return new SerializableChunkData(
            factory, new ChunkPos(-7, 9), -4, 0L, 0L, ChunkStatus.EMPTY,
            null, retroGen, UpgradeData.EMPTY, null, Map.of(Heightmap.Types.WORLD_SURFACE, new long[20]),
            new ChunkAccess.PackedTicks(List.of(), List.of()), new ShortList[1], false,
            List.of(new SerializableChunkData.SectionData(-4, new LevelChunkSection(factory), null, null)),
            List.of(), List.of(), new CompoundTag()
        );
    }
}
