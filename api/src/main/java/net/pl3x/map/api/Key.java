package net.pl3x.map.api;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * Simple string wrapper used to identify things. Equality is checked based only on the key string.
 *
 * <p>Valid characters for keys are <a href="https://regexr.com/5ks7f">{@code [a-z0-9._-]}</a>.</p>
 *
 * <p>Keys should be unique, so prefixing keys with a plugin name, for example {@code "myplugin_marker-1"}, would be good practice.</p>
 */
public final class Key {

    private static final IntPredicate CHARACTER_PREDICATE = value -> value == '_' || value == '-' || (value >= 'a' && value <= 'z') || (value >= '0' && value <= '9') || value == '.';

    private final String key;

    private Key(final @NonNull String key) {
        if (!validKey(key)) throw invalidKey(key);
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

    private static boolean validKey(final @NonNull String key) {
        for (int i = 0, length = key.length(); i < length; i++) {
            if (!CHARACTER_PREDICATE.test(key.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static @NonNull IllegalArgumentException invalidKey(final @NonNull String key) {
        return new IllegalArgumentException(String.format(
                "Non [a-z0-9._-] character in key '%s'",
                key
        ));
    }

}
