package net.pl3x.map.api;

import java.util.UUID;

public interface PlayerManager {

    /**
     * Hide player on map
     *
     * @param uuid UUID of player to hide
     */
    void hide(UUID uuid);

    /**
     * Show player on map
     *
     * @param uuid UUID of player to show
     */
    void show(UUID uuid);

    /**
     * Check if player is hidden on map
     *
     * @param uuid UUID of player to check
     * @return true if player is hidden
     */
    boolean isHidden(UUID uuid);

}
