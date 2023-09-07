package xyz.jpenilla.squaremap.api;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * squaremap API
 *
 * <p>The API allows other plugins on the server integrate with squaremap.</p>
 *
 * <p>This interface represents the base of the API package. All functions are
 * accessed via this interface.</p>
 *
 * <p>To start using the API, you need to obtain an instance of this interface.
 * These are registered by the squaremap plugin to the platforms Services
 * Manager. This is the preferred method for obtaining an instance.</p>
 *
 * <p>For ease of use, an instance can also be obtained from the static
 * singleton accessor in {@link SquaremapProvider}.</p>
 */
public interface Squaremap {

    /**
     * Get an unmodifiable view of the enabled worlds
     *
     * @return The set of worlds
     */
    @NonNull Collection<MapWorld> mapWorlds();

    /**
     * Get an optional which will either
     * <ul>
     *     <li>A) Be empty, if the world does not exist, or does not have squaremap enabled</li>
     *     <li>B) Contain the {@link MapWorld} instance for the World associated with the provided {@link WorldIdentifier}, if the world exists and has squaremap enabled</li>
     * </ul>
     *
     * @param identifier world identifier
     * @return optional
     */
    @NonNull Optional<MapWorld> getWorldIfEnabled(@NonNull WorldIdentifier identifier);

    /**
     * Get the registry of images which can be used with icon markers
     *
     * @return icon registry
     */
    @NonNull Registry<BufferedImage> iconRegistry();

    /**
     * Get the player manager
     *
     * @return player manager
     */
    @NonNull PlayerManager playerManager();

    /**
     * Get the web directory
     *
     * @return web directory
     */
    @NonNull Path webDir();

    /**
     * Get an {@link HtmlComponentSerializer} using the platform {@link ComponentFlattener}.
     *
     * @return serializer
     * @since 1.2.0
     */
    @NonNull HtmlComponentSerializer htmlComponentSerializer();

}
