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
        hide(player(uuid));
    }

    public void hide(final @NonNull Player player) {
        hide(pdc(player));
    }

    public void hide(final @NonNull PersistentDataContainer pdc) {
        pdc.set(key, PersistentDataType.BYTE, (byte) 1);
    }

    @Override
    public void show(final @NonNull UUID uuid) {
        show(player(uuid));
    }

    public void show(final @NonNull Player player) {
        show(pdc(player));
    }

    public void show(final @NonNull PersistentDataContainer pdc) {
        pdc.set(key, PersistentDataType.BYTE, (byte) 0);
    }

    @Override
    public void hidden(@NonNull UUID uuid, final boolean hide) {
        hidden(player(uuid), hide);
    }

    public void hidden(@NonNull Player player, final boolean hide) {
        if (hide) {
            this.hide(player);
        } else {
            this.show(player);
        }
    }

    @Override
    public boolean hidden(final @NonNull UUID uuid) {
        return hidden(player(uuid));
    }

    public boolean hidden(final @NonNull Player player) {
        return hidden(pdc(player));
    }

    public boolean hidden(final @NonNull PersistentDataContainer pdc) {
        return pdc.getOrDefault(key, PersistentDataType.BYTE, (byte) 0) != (byte) 0;
    }

    private static Player player(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            throw new IllegalArgumentException("Player is not online");
        }
        return player;
    }

    private static PersistentDataContainer pdc(Player player) {
        return player.getPersistentDataContainer();
    }
}
