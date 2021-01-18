package net.pl3x.map.api;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Pl3xMap API
 *
 * <p>The API allows other plugins on the server integrate with Pl3xmap.</p>
 *
 * <p>This interface represents the base of the API package. All functions are
 * accessed via this interface.</p>
 *
 * <p>To start using the API, you need to obtain an instance of this interface.
 * These are registered by the Pl3xMap plugin to the platforms Services
 * Manager. This is the preferred method for obtaining an instance.</p>
 *
 * <p>For ease of use, an instance can also be obtained from the static
 * singleton accessor in {@link Pl3xMapProvider}.</p>
 */
public interface Pl3xMap {

    /**
     * Get the layer registry
     *
     * @return the layer registry
     */
    @NonNull Registry<LayerProvider> layerRegistry();

}
