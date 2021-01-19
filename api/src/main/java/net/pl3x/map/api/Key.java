package net.pl3x.map.api;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

/**
 * Simple string wrapper used to identify things. Equality is checked based only on the key string.
 */
public final class Key {

    private final String key;

    private Key(final @NonNull String key) {
        this.key = key;
    }

    /**
     * Get a new Key instance for the provided key string.
     *
     * @param key string for the key
     * @return new key
     */
    public static @NonNull Key of(final @NonNull String key) {
        return new Key(key);
    }

    /**
     * Get a new Key instance for the provided key string.
     *
     * @param key string for the key
     * @return new key
     */
    public static @NonNull Key key(final @NonNull String key) {
        return new Key(key);
    }

    /**
     * Get the key string for this key
     *
     * @return key string
     */
    public @NonNull String getKey() {
        return this.key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Key key1 = (Key) o;
        return this.key.equals(key1.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public @NonNull String toString() {
        return this.key;
    }

}
