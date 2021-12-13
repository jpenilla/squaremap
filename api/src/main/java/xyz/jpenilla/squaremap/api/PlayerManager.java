package xyz.jpenilla.squaremap.api;

import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Interface for interacting with players on the map
 */
public interface PlayerManager {

    /**
     * Set a player to be temporarily hidden on the map
     * <p>
     * The status will last until the server restarts or the plugin reloads
     *
     * @param uuid player UUID
     */
    default void hide(@NonNull UUID uuid) {
        hide(uuid, false);
    }

    /**
     * Set a player to be hidden on the map
     *
     * @param uuid       player UUID
     * @param persistent persist the hide status
     */
    void hide(@NonNull UUID uuid, boolean persistent);

    /**
     * Set a player to temporarily not be hidden on the map
     * <p>
     * The status will last until the server restarts or the plugin reloads
     *
     * @param uuid player UUID
     */
    default void show(@NonNull UUID uuid) {
        show(uuid, false);
    }

    /**
     * Set a player to not be hidden on the map
     *
     * @param uuid       player UUID
     * @param persistent persist the show status
     */
    void show(@NonNull UUID uuid, boolean persistent);

    /**
     * Set whether a player is hidden on the map temporarily
     * <p>
     * The status will last until the server restarts or the plugin reloads
     *
     * @param uuid player UUID
     * @param hide whether to hide the player
     */
    default void hidden(@NonNull UUID uuid, boolean hide) {
        hidden(uuid, hide, false);
    }

    /**
     * Set whether a player is hidden on the map
     *
     * @param uuid       player UUID
     * @param hide       whether to hide the player
     * @param persistent persist the hide status
     */
    void hidden(@NonNull UUID uuid, boolean hide, boolean persistent);

    /**
     * Get whether a player is hidden on the map
     *
     * @param uuid player UUID
     * @return whether the player is hidden
     */
    boolean hidden(@NonNull UUID uuid);

}
