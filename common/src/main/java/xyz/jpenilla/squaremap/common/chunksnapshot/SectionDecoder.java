package xyz.jpenilla.squaremap.common.chunksnapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;
import java.util.stream.LongStream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * Decodes saved sections with the same results as vanilla's container codecs, decoding each
 * distinct palette entry once. Thread-safe.
 */
@DefaultQualifier(NonNull.class)
public final class SectionDecoder {
    private static final String PALETTE = "palette";
    private static final String DATA = "data";

    private final CachingPaletteDecoder<BlockState> blockStates;
    private final int blockEntries;
    private final CachingPaletteDecoder<Holder<Biome>> biomes;
    private final int biomeEntries;

    public SectionDecoder(final PalettedContainerFactory containerFactory, final Registry<Biome> biomeRegistry) {
        this.blockStates = new CachingPaletteDecoder<>(BlockState.CODEC, containerFactory.defaultBlockState());
        this.blockEntries = containerFactory.blockStatesStrategy().entryCount();
        this.biomes = new CachingPaletteDecoder<>(biomeRegistry.holderByNameCodec(), containerFactory.defaultBiome());
        this.biomeEntries = containerFactory.biomeStrategy().entryCount();
    }

    public static SectionDecoder create(final ServerLevel level) {
        return new SectionDecoder(level.palettedContainerFactory(), level.registryAccess().lookupOrThrow(Registries.BIOME));
    }

    /**
     * Decodes a section's block states.
     *
     * @param container the saved {@code block_states} tag
     * @return the block states
     */
    DataResult<SectionData<BlockState>> blockStates(final CompoundTag container) {
        return this.blockStates.resolvePalette(container, BlockState[]::new).flatMap(palette -> {
            // Matches Strategy.createForBlockStates: at least 4 bits once there is more than one entry.
            final int bits = palette.length == 1 ? 0 : Math.max(4, Mth.ceillog2(palette.length));
            return packed(palette, container, bits, this.blockEntries);
        });
    }

    /**
     * Decodes a section's biomes.
     *
     * @param container the saved {@code biomes} tag
     * @return the biomes
     */
    @SuppressWarnings("unchecked")
    DataResult<SectionData<Holder<Biome>>> biomes(final CompoundTag container) {
        final DataResult<Holder<Biome>[]> palette = this.biomes.resolvePalette(container, Holder[]::new);
        // Matches Strategy.createForBiomes, which stores the minimum bits for the palette size.
        return palette.flatMap(entries -> packed(entries, container, Mth.ceillog2(entries.length), this.biomeEntries));
    }

    private static <T> DataResult<SectionData<T>> packed(final T[] palette, final CompoundTag container, final int bits, final int entryCount) {
        if (bits == 0) {
            // Vanilla ignores data for single-value palettes.
            return DataResult.success(new SectionData.Packed<>(palette, null));
        }
        final long @Nullable [] data = data(container.get(DATA));
        if (data == null) {
            return DataResult.error(() -> "Missing values for non-zero storage");
        }
        try {
            return DataResult.success(new SectionData.Packed<>(palette, new SimpleBitStorage(bits, entryCount, data)));
        } catch (final SimpleBitStorage.InitializationException e) {
            return DataResult.error(() -> "Failed to read PalettedContainer: " + e.getMessage());
        }
    }

    private static long @Nullable [] data(final @Nullable Tag tag) {
        if (tag instanceof LongArrayTag longArray) {
            return longArray.getAsLongArray();
        }
        if (tag == null) {
            return null;
        }
        // Vanilla also accepts other numeric lists, and its lenient data field treats unreadable data as missing.
        return NbtOps.INSTANCE.getLongStream(tag).result().map(LongStream::toArray).orElse(null);
    }

    // Caches entries that decode without errors, keyed by their tags, which compare by value.
    private record CachingPaletteDecoder<T>(Codec<T> codec, T defaultValue, Map<Tag, T> decoded) {
        CachingPaletteDecoder(final Codec<T> codec, final T defaultValue) {
            this(codec, defaultValue, new ConcurrentHashMap<>());
        }

        DataResult<T[]> resolvePalette(final CompoundTag container, final IntFunction<T[]> newArray) {
            if (!(container.get(PALETTE) instanceof ListTag paletteTag) || paletteTag.isEmpty()) {
                return DataResult.error(() -> "Missing or empty palette");
            }
            final T[] palette = newArray.apply(paletteTag.size());
            @Nullable StringJoiner errors = null;
            for (int i = 0; i < palette.length; i++) {
                final Tag entry = paletteTag.get(i);
                final @Nullable T cached = this.decoded.get(entry);
                if (cached != null) {
                    palette[i] = cached;
                    continue;
                }
                final DataResult<T> result = this.codec.parse(NbtOps.INSTANCE, entry);
                if (result instanceof DataResult.Error<T> error) {
                    // Like ExtraCodecs.orElsePartial in vanilla's container codecs: the partial result if any, otherwise the default.
                    if (errors == null) {
                        errors = new StringJoiner("; ");
                    }
                    if (error.partialValue().isPresent()) {
                        palette[i] = error.partialValue().get();
                        errors.add(error.message());
                    } else {
                        palette[i] = this.defaultValue;
                        errors.add("(" + error.message() + " -> using default)");
                    }
                } else {
                    final T value = result.getOrThrow();
                    palette[i] = value;
                    this.decoded.put(entry, value);
                }
            }
            if (errors != null) {
                return DataResult.error(errors::toString, palette);
            }
            return DataResult.success(palette);
        }
    }
}
