package net.pl3x.map.api;

import net.pl3x.map.api.visibilitylimit.VisibilityLimit;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;

/**
 * Represents a world which is mapped by Pl3xMap
 */
public interface MapWorld {

    /**
     * Get the layer registry for this world
     *
     * @return the layer registry
     */
    @NonNull Registry<LayerProvider> layerRegistry();

    /**
     * Get the name of this world
     *
     * @return name
     */
    @NonNull String name();

    /**
     * Get the map visibility limit of the world. Only these regions are drawn,
     * even if more chunks exist on disk.
     * @return The visibility limit.
     */
    @NonNull VisibilityLimit visibilityLimit();

    /**
     * Get the UUID of this world
     *
     * @return uuid
     */
    @NonNull UUID uuid();

}
