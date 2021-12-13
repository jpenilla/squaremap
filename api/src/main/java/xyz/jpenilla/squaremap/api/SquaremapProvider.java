package xyz.jpenilla.squaremap.api;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Static singleton for conveniently accessing the squaremap API instance.
 * Prefer using the platform's service manager when available.
 */
public final class SquaremapProvider {
    private static Squaremap instance = null;

    private SquaremapProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Gets an instance of the {@link Squaremap} service,
     * throwing {@link IllegalStateException} if an instance is not yet loaded.
     *
     * <p>Will never return null.</p>
     *
     * @return an api instance
     * @throws IllegalStateException if the api is not loaded
     */
    public static @NonNull Squaremap get() {
        if (instance == null) {
            throw new IllegalStateException("The squaremap API is not loaded.");
        }
        return instance;
    }

    static void register(final @NonNull Squaremap instance) {
        SquaremapProvider.instance = instance;
    }

    static void unregister() {
        SquaremapProvider.instance = null;
    }
}
