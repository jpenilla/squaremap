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
     * Get the identifier of this world.
     *
     * @return identifier
     */
    @NonNull WorldIdentifier identifier();

}
