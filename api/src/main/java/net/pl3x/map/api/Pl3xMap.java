package net.pl3x.map.api;

import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Optional;

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
     * Get an unmodifiable view of the enabled worlds
     *
     * @return The set of worlds
     */
    @NonNull Collection<MapWorld> mapWorlds();

    /**
     * Get an optional which will either
     * <ul>
     *     <li>A) Be empty, if the world does not have Pl3xMap enabled</li>
     *     <li>B) Contain the {@link MapWorld} instance for the provided {@link World}, if the world does have Pl3xMap enabled</li>
     * </ul>
     *
     * @param world Bukkit World
     * @return optional
     */
    @NonNull Optional<MapWorld> getWorldIfEnabled(@NonNull World world);

    /**
     * Get the registry of images which can be used with icon markers
     *
     * @return icon registry
     */
    @NonNull Registry<BufferedImage> iconRegistry();

}
