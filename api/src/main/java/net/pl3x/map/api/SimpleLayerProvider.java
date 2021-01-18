package net.pl3x.map.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A simple {@link LayerProvider} implementation, backed by a Map.
 */
public final class SimpleLayerProvider implements LayerProvider {

    private final Supplier<String> labelSupplier;
    private final Map<Key, Marker> markers = new ConcurrentHashMap<>();

    private SimpleLayerProvider(final @NonNull Supplier<String> labelSupplier) {
        this.labelSupplier = labelSupplier;
    }

    /**
     * Get a new {@link SimpleLayerProvider.Builder} instance
     *
     * @param label label for the layer
     * @return new builder
     */
    public static SimpleLayerProvider.@NonNull Builder builder(final @NonNull String label) {
        return new Builder(() -> label);
    }

    /**
     * Get a new {@link SimpleLayerProvider.Builder} instance
     *
     * @param labelSupplier label supplier for the layer
     * @return new builder
     */
    public static SimpleLayerProvider.@NonNull Builder builder(final @NonNull Supplier<String> labelSupplier) {
        return new Builder(labelSupplier);
    }

    /**
     * Add a new marker to this layer
     *
     * @param key    key
     * @param marker marker
     */
    public void addMarker(final @NonNull Key key, final @NonNull Marker marker) {
        this.markers.put(key, marker);
    }

    /**
     * Remove an existing marker from this layer, returning either the removed marker, or {@code null} if
     * no marker was present for the provided key.
     *
     * @param key key
     * @return the existing marker or {@code null}
     */
    public @Nullable Marker removeMarker(final @NonNull Key key) {
        return this.markers.remove(key);
    }

    /**
     * Remove all registered markers
     */
    public void clearMarkers() {
        this.markers.clear();
    }

    /**
     * Get an unmodifiable view of the registered markers
     *
     * @return registered markers
     */
    public @NonNull Map<Key, Marker> registeredMarkers() {
        return Collections.unmodifiableMap(this.markers);
    }

    /**
     * Check whether a marker is registered for a key
     *
     * @param key key
     * @return whether a marker is registered
     */
    public boolean hasMarker(final @NonNull Key key) {
        return this.markers.containsKey(key);
    }

    @Override
    public @NonNull String getLabel() {
        return this.labelSupplier.get();
    }

    @Override
    public @NonNull Collection<Marker> getMarkers() {
        return this.markers.values();
    }

    /**
     * Builder for {@link SimpleLayerProvider}
     */
    public static final class Builder {
        private final Supplier<String> labelSupplier;

        private Builder(final @NonNull Supplier<String> labelSupplier) {
            this.labelSupplier = labelSupplier;
        }

        /**
         * Build a {@link SimpleLayerProvider} instance from the current state of this builder
         *
         * @return the built instance
         */
        public @NonNull SimpleLayerProvider build() {
            return new SimpleLayerProvider(this.labelSupplier);
        }
    }

}
