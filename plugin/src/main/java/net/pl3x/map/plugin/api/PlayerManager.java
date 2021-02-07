package net.pl3x.map.plugin.api;

import net.pl3x.map.plugin.Pl3xMapPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;

public class PlayerManager implements net.pl3x.map.api.PlayerManager {
    public static final NamespacedKey key = new NamespacedKey(Pl3xMapPlugin.getInstance(), "hidden");

    @Override
    public void hide(final @NonNull UUID uuid) {
        pdc(uuid).set(key, PersistentDataType.BYTE, (byte) 1);
    }

    @Override
    public void show(final @NonNull UUID uuid) {
        pdc(uuid).set(key, PersistentDataType.BYTE, (byte) 0);
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
        return pdc(uuid).getOrDefault(key, PersistentDataType.BYTE, (byte) 0) != (byte) 0;
    }

    private static PersistentDataContainer pdc(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            throw new IllegalArgumentException("Player is not online");
        }
        return player.getPersistentDataContainer();
    }
}
