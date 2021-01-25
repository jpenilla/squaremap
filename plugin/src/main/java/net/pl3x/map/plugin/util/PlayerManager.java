package net.pl3x.map.plugin.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerManager implements net.pl3x.map.api.PlayerManager {
    private final Set<UUID> hiddenPlayers = new HashSet<>();

    @Override
    public void hide(UUID uuid) {
        hiddenPlayers.add(uuid);
    }

    @Override
    public void show(UUID uuid) {
        hiddenPlayers.remove(uuid);
    }

    @Override
    public boolean isHidden(UUID uuid) {
        return hiddenPlayers.contains(uuid);
    }
}
