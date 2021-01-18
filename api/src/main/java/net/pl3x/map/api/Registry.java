package net.pl3x.map.api;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Simple registry interface
 *
 * @param <T> Generic type argument
 */
public interface Registry<T> {

    /**
     * Register a new entry with the provided key and value
     *
     * @param key   key
     * @param value value
     * @throws IllegalArgumentException when there is already an entry for the provided key
     */
    void register(@NonNull Key key, @NonNull T value);

    /**
     * Unregister the entry for the provided key if present
     *
     * @param key key
     * @throws IllegalArgumentException when there is no entry for the provided key
     */
    void unregister(@NonNull Key key);

    /**
     * Check whether an entry is present for the provided key
     *
     * @param key key
     * @return whether an entry is present
     */
    boolean hasEntry(@NonNull Key key);

    /**
     * Get the registered value for a key
     *
     * @param key key
     * @return value
     * @throws IllegalArgumentException when there is no value for the provided key
     */
    @NonNull T get(@NonNull Key key);

    /**
     * Get the registered entries
     *
     * @return the registered entries
     */
    @NonNull Iterable<Pair<Key, T>> entries();

}
