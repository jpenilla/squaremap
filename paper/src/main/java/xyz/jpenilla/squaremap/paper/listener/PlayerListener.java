package xyz.jpenilla.squaremap.paper.listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.paper.network.Network;

public final class PlayerListener implements Listener {
    public static final Set<UUID> clientUsers = new HashSet<>();

    @EventHandler
    public void handlePlayerQuit(final @NonNull PlayerQuitEvent event) {
        clientUsers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void handlePlayerChangedWorld(final @NonNull PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (clientUsers.contains(player.getUniqueId())) {
            Network.sendUpdateWorld(player);
        }
    }
}
