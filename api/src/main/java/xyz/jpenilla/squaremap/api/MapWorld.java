package xyz.jpenilla.squaremap.api;

import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a world mapped by squaremap.
 */
public interface MapWorld {

    /**
     * Gets the layer registry for this world.
     *
     * @return the layer registry
     */
    @NonNull Registry<LayerProvider> layerRegistry();

    /**
     * Get the name of this world.
     *
     * @return name
     */
    @NonNull String name();

    /**
     * Get the map visibility limit of the world. Only these regions are drawn,
     * even if more chunks exist on disk.
     *
     * @return The visibility limit.
     */
    //@NonNull VisibilityLimit visibilityLimit();

    /**
     * Get the UUID of this world
     *
     * @return uuid
     */
    @NonNull UUID uuid();

}
