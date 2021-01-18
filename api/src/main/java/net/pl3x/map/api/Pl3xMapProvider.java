package net.pl3x.map.api;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Static singleton for conveniently accessing the Pl3xMap API instance
 */
public final class Pl3xMapProvider {
    private static Pl3xMap instance = null;

    private Pl3xMapProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Gets an instance of the {@link Pl3xMap} service,
     * throwing {@link IllegalStateException} if an instance is not yet loaded.
     *
     * <p>Will never return null.</p>
     *
     * @return an api instance
     * @throws IllegalStateException if the api is not loaded
     */
    public static @NonNull Pl3xMap get() {
        if (instance == null) {
            throw new IllegalStateException("The Pl3xMap API is not loaded.");
        }
        return instance;
    }

    static void register(final @NonNull Pl3xMap instance) {
        Pl3xMapProvider.instance = instance;
    }

    static void unregister() {
        Pl3xMapProvider.instance = null;
    }

}
