package net.pl3x.map.plugin.api;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager implements net.pl3x.map.api.PlayerManager {
    private final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public void hide(final @NonNull UUID uuid) {
        this.hiddenPlayers.add(uuid);
    }

    @Override
    public void show(final @NonNull UUID uuid) {
        this.hiddenPlayers.remove(uuid);
    }

    @Override
    public void hidden(@NonNull UUID uuid, final boolean hide) {
        if (hide) {
            this.hide(uuid);
        } else {
            this.show(uuid);
        }
    }

    @Override
    public boolean hidden(final @NonNull UUID uuid) {
        return this.hiddenPlayers.contains(uuid);
    }
}
