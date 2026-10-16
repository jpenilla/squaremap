package xyz.jpenilla.squaremap.common.chunksnapshot;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Compares {@link SectionDecoder} against vanilla's container codecs on the same section data.
 */
class SectionDecoderTest {
    private static PalettedContainerFactory factory;
    private static SectionDecoder decoder;
    private static List<BlockState> allStates;
    private static List<Holder<Biome>> allBiomes;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        final var registries = VanillaRegistries.createWorldLookup();
        final MappedRegistry<Biome> biomes = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        registries.lookupOrThrow(Registries.BIOME).listElements()
            .forEach(biome -> biomes.register(biome.key(), biome.value(), RegistrationInfo.BUILT_IN));
        biomes.freeze();
        factory = PalettedContainerFactory.create(new RegistryAccess.ImmutableRegistryAccess(List.of(biomes)));
        decoder = new SectionDecoder(factory, biomes);
        allStates = new ArrayList<>();
        Block.BLOCK_STATE_REGISTRY.forEach(allStates::add);
        allBiomes = new ArrayList<>();
        factory.biomeStrategy().globalMap().forEach(allBiomes::add);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 5, 16, 17, 100, 256, 257, 1000})
    void decodesBlockStatesLikeVanilla(final int paletteSize) {
        assertBlockStatesMatch(blockStatesTag(paletteSize, paletteSize));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 8, 9, 30})
    void decodesBiomesLikeVanilla(final int paletteSize) {
        assertBiomesMatch(biomesTag(paletteSize));
    }

    @Test
    void unknownBlockUsesDefault() {
        final CompoundTag tag = blockStatesTag(5, 5);
        tag.getListOrEmpty("palette").set(2, StringTag.valueOf("squaremap_test:missing"));
        assertBlockStatesMatch(tag);
    }

    @Test
    void unknownBiomeUsesDefault() {
        final CompoundTag tag = biomesTag(3);
        tag.getListOrEmpty("palette").set(1, StringTag.valueOf("squaremap_test:missing"));
        assertBiomesMatch(tag);
    }

    @Test
    void missingDataMatchesVanilla() {
        final CompoundTag tag = blockStatesTag(5, 5);
        tag.remove("data");
        assertThrows(SerializableChunkData.ChunkReadException.class, () -> vanillaBlockStates(tag));
        assertThrows(SerializableChunkData.ChunkReadException.class, () -> decodeBlockStates(tag));
    }

    @Test
    void wrongDataLengthMatchesVanilla() {
        final CompoundTag tag = blockStatesTag(5, 5);
        tag.putLongArray("data", new long[10]);
        assertThrows(SerializableChunkData.ChunkReadException.class, () -> vanillaBlockStates(tag));
        assertThrows(SerializableChunkData.ChunkReadException.class, () -> decodeBlockStates(tag));
    }

    @Test
    void listDataMatchesVanilla() {
        final CompoundTag tag = blockStatesTag(5, 5);
        final ListTag data = new ListTag();
        for (final long value : tag.getLongArray("data").orElseThrow()) {
            data.add(LongTag.valueOf(value));
        }
        tag.put("data", data);
        assertBlockStatesMatch(tag);
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 300})
    void outOfRangeIndexMatchesVanilla(final int paletteSize) {
        final CompoundTag tag = blockStatesTag(paletteSize, paletteSize);
        final long[] data = tag.getLongArray("data").orElseThrow();
        final int bits = paletteSize <= 16 ? 4 : 9;
        // Set the first value to the highest index the bits can hold, past the end of the palette.
        data[0] = data[0] & ~((1L << bits) - 1) | (1L << bits) - 1;
        tag.putLongArray("data", data);
        // Vanilla fails on read for small palettes, but repacks global ones on decode and throws there.
        // Our decoder never repacks, so it fails on read for both, with the same exception.
        if (paletteSize <= 256) {
            final SectionData<BlockState> vanilla = vanillaBlockStates(tag);
            assertThrows(MissingPaletteEntryException.class, () -> vanilla.get(0));
        } else {
            assertThrows(MissingPaletteEntryException.class, () -> vanillaBlockStates(tag));
        }
        final SectionData<BlockState> decoded = decodeBlockStates(tag);
        assertThrows(MissingPaletteEntryException.class, () -> decoded.get(0));
    }

    @Test
    void emptyPaletteFails() {
        final CompoundTag tag = new CompoundTag();
        tag.put("palette", new ListTag());
        // Vanilla decodes this, but every read from the section throws.
        assertThrows(SerializableChunkData.ChunkReadException.class, () -> decodeBlockStates(tag));
    }

    private static CompoundTag blockStatesTag(final int paletteSize, final long seed) {
        final Random random = new Random(seed);
        final List<BlockState> states = new ArrayList<>(allStates);
        Collections.shuffle(states, random);
        final PalettedContainer<BlockState> container = factory.createForBlockStates();
        // Use every chosen state at least once, so the written palette has exactly paletteSize entries.
        for (int i = 0; i < 4096; i++) {
            final BlockState state = states.get(i < paletteSize ? i : random.nextInt(paletteSize));
            container.set(i & 15, i >> 8, i >> 4 & 15, state);
        }
        final CompoundTag tag = (CompoundTag) factory.blockStatesContainerCodec().encodeStart(NbtOps.INSTANCE, container).getOrThrow();
        assertEquals(paletteSize, tag.getListOrEmpty("palette").size());
        return tag;
    }

    private static CompoundTag biomesTag(final int paletteSize) {
        final Random random = new Random(paletteSize);
        final PalettedContainer<Holder<Biome>> container = factory.createForBiomes();
        for (int i = 0; i < 64; i++) {
            final Holder<Biome> biome = allBiomes.get(i < paletteSize ? i : random.nextInt(paletteSize));
            container.set(i & 3, i >> 4, i >> 2 & 3, biome);
        }
        final CompoundTag tag = (CompoundTag) factory.biomeContainerCodec().encodeStart(NbtOps.INSTANCE, container).getOrThrow();
        assertEquals(paletteSize, tag.getListOrEmpty("palette").size());
        return tag;
    }

    // Like ChunkSnapshotFactory, without logging.
    private static <T> T orThrow(final DataResult<T> result) {
        return result.promotePartial(msg -> {}).getOrThrow(SerializableChunkData.ChunkReadException::new);
    }

    private static SectionData<BlockState> decodeBlockStates(final CompoundTag tag) {
        return orThrow(decoder.blockStates(tag));
    }

    private static SectionData<Holder<Biome>> decodeBiomes(final CompoundTag tag) {
        return orThrow(decoder.biomes(tag));
    }

    private static SectionData<BlockState> vanillaBlockStates(final CompoundTag tag) {
        return SectionData.of(orThrow(factory.blockStatesContainerCodec().parse(NbtOps.INSTANCE, tag)));
    }

    private static SectionData<Holder<Biome>> vanillaBiomes(final CompoundTag tag) {
        return SectionData.of((PalettedContainer<Holder<Biome>>) orThrow(factory.biomeContainerCodec().parse(NbtOps.INSTANCE, tag)));
    }

    private static void assertBlockStatesMatch(final CompoundTag tag) {
        assertSectionDataMatch(vanillaBlockStates(tag), decodeBlockStates(tag), 4096);
    }

    private static void assertBiomesMatch(final CompoundTag tag) {
        assertSectionDataMatch(vanillaBiomes(tag), decodeBiomes(tag), 64);
    }

    // For fixtures that decode; any exception fails the test.
    private static <T> void assertSectionDataMatch(final SectionData<T> expected, final SectionData<T> actual, final int size) {
        for (int i = 0; i < size; i++) {
            assertEquals(expected.get(i), actual.get(i), "Index " + i);
        }
        assertEquals(
            expected.maybeHas(value -> !(value instanceof BlockState state) || !state.isAir()),
            actual.maybeHas(value -> !(value instanceof BlockState state) || !state.isAir())
        );
    }
}
