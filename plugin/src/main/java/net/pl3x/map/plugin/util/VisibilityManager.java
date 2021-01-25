package net.pl3x.map.plugin.util;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VisibilityManager implements net.pl3x.map.api.VisibilityManager {
    private final Set<UUID> hiddenPlayers = new HashSet<>();

    @Override
    public void hide(Player player) {
        hide(player.getUniqueId());
    }

    @Override
    public void hide(UUID uuid) {
        hiddenPlayers.add(uuid);
    }

    @Override
    public void show(Player player) {
        show(player.getUniqueId());
    }

    @Override
    public void show(UUID uuid) {
        hiddenPlayers.remove(uuid);
    }

    @Override
    public boolean isHidden(Player player) {
        return isHidden(player.getUniqueId());
    }

    @Override
    public boolean isHidden(UUID uuid) {
        return hiddenPlayers.contains(uuid);
    }
}
