package net.pl3x.map.api;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface VisibilityManager {

    /**
     * Hide player on map
     *
     * @param player Player to hide
     */
    void hide(Player player);

    /**
     * Hide player on map
     *
     * @param uuid UUID of player to hide
     */
    void hide(UUID uuid);

    /**
     * Show player on map
     *
     * @param player Player to show
     */
    void show(Player player);

    /**
     * Show player on map
     *
     * @param uuid UUID of player to show
     */
    void show(UUID uuid);

    /**
     * Check if player is hidden on map
     *
     * @param player Player to check
     * @return true if player is hidden
     */
    boolean isHidden(Player player);

    /**
     * Check if player is hidden on map
     *
     * @param uuid UUID of player to check
     * @return true if player is hidden
     */
    boolean isHidden(UUID uuid);

}
