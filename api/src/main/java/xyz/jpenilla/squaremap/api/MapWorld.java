package xyz.jpenilla.squaremap.api;

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
     * Get the name of this world. Depending on the implementation,
     * this may be the same as the {@link WorldIdentifier#asString()} of {@link #identifier()}.
     *
     * @return name
     */
    @NonNull String name();

    /**
     * Get the identifier of this world.
     *
     * @return identifier
     */
    @NonNull WorldIdentifier identifier();

    /**
     * Get the map visibility limit of the world. Only these regions are drawn,
     * even if more chunks exist on disk.
     *
     * @return The visibility limit.
     */
    //@NonNull VisibilityLimit visibilityLimit();

}
