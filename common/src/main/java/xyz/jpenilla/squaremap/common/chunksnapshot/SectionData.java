package xyz.jpenilla.squaremap.common.chunksnapshot;

import java.util.function.Predicate;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * Read-only block states or biomes of a chunk section.
 *
 * @param <T> the value type
 */
@DefaultQualifier(NonNull.class)
sealed interface SectionData<T> permits SectionData.Container, SectionData.Packed {
    /**
     * Gets a value by its index, as in {@link PalettedContainer#get(int)}.
     *
     * @param index the index
     * @return the value
     */
    T get(int index);

    /**
     * Checks the palette, like {@link PalettedContainer#maybeHas(Predicate)}.
     *
     * @param predicate the predicate
     * @return whether any palette entry matches
     */
    boolean maybeHas(Predicate<T> predicate);

    static <T> SectionData<T> of(final PalettedContainer<T> container) {
        return new Container<>(container);
    }

    record Container<T>(PalettedContainer<T> container) implements SectionData<T> {
        @Override
        public T get(final int index) {
            return this.container.get(index);
        }

        @Override
        public boolean maybeHas(final Predicate<T> predicate) {
            return this.container.maybeHas(predicate);
        }
    }

    // Storage is null for a single-value palette.
    record Packed<T>(T[] palette, @Nullable SimpleBitStorage storage) implements SectionData<T> {
        @SuppressWarnings("unchecked")
        static <T> Packed<T> single(final T value) {
            return new Packed<>((T[]) new Object[]{value}, null);
        }

        @Override
        public T get(final int index) {
            if (this.storage == null) {
                return this.palette[0];
            }
            final int id = this.storage.get(index);
            if (id >= this.palette.length) {
                // Like vanilla's linear and hash map palettes, which also fail on read.
                throw new MissingPaletteEntryException(id);
            }
            return this.palette[id];
        }

        @Override
        public boolean maybeHas(final Predicate<T> predicate) {
            for (final T value : this.palette) {
                if (predicate.test(value)) {
                    return true;
                }
            }
            return false;
        }
    }
}
