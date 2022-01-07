package xyz.jpenilla.squaremap.paper.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.network.Networking;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

public final class PlayerListener implements Listener {
    @EventHandler
    public void handlePlayerQuit(final @NonNull PlayerQuitEvent event) {
        Networking.CLIENT_USERS.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void handlePlayerChangedWorld(final @NonNull PlayerChangedWorldEvent event) {
        Networking.worldChanged(CraftBukkitReflection.serverPlayer(event.getPlayer()));
    }
}
