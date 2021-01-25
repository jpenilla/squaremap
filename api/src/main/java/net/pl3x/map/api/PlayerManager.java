package net.pl3x.map.api;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;

/**
 * Interface for interacting with players on the map
 */
public interface PlayerManager {

    /**
     * Set a player to be hidden on the map
     *
     * @param uuid player UUID
     */
    void hide(@NonNull UUID uuid);

    /**
     * Set a player to not be hidden on the map
     *
     * @param uuid player UUID
     */
    void show(@NonNull UUID uuid);

    /**
     * Set whether a player is hidden on the map
     *
     * @param uuid player UUID
     * @param hide whether to hide the player
     */
    void hidden(@NonNull UUID uuid, boolean hide);

    /**
     * Get whether a player is hidden on the map
     *
     * @param uuid player UUID
     * @return whether the player is hidden
     */
    boolean hidden(@NonNull UUID uuid);

}
