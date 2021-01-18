package net.pl3x.map.api;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;

/**
 * Provides Markers and other metadata which make up a layer. LayerProviders are called on each update of a layer.
 */
public interface LayerProvider {

    /**
     * Get the label of this LayerProvider
     *
     * @return label
     */
    @NonNull String getLabel();

    /**
     * Get the collection of markers to display
     *
     * @return markers
     */
    @NonNull Collection<Marker> getMarkers();

}
