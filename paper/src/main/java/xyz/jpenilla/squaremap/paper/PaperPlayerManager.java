package xyz.jpenilla.squaremap.paper;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.common.PlayerManagerInternal;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

public final class PaperPlayerManager implements PlayerManagerInternal {
    public static final NamespacedKey key = new NamespacedKey(SquaremapPlugin.getInstance(), "hidden");

    private final Set<UUID> tempHidden = ConcurrentHashMap.newKeySet();

    @Override
    public void hide(final @NonNull UUID uuid, boolean persistent) {
        this.hide(getPlayer(uuid), persistent);
    }

    public void hide(final @NonNull Player player, boolean persistent) {
        if (persistent) {
            pdc(player).set(key, PersistentDataType.BYTE, (byte) 1);
        }
        this.tempHidden.add(player.getUniqueId());
    }

    @Override
    public void show(final @NonNull UUID uuid, boolean persistent) {
        this.show(getPlayer(uuid), persistent);
    }

    public void show(final @NonNull Player player, boolean persistent) {
        if (persistent) {
            pdc(player).set(key, PersistentDataType.BYTE, (byte) 0);
        }
        this.tempHidden.remove(player.getUniqueId());
    }

    @Override
    public void hidden(@NonNull UUID uuid, final boolean hide, boolean persistent) {
        this.hidden(getPlayer(uuid), hide, persistent);
    }

    public void hidden(@NonNull Player player, final boolean hide, boolean persistent) {
        if (hide) {
            this.hide(player, persistent);
        } else {
            this.show(player, persistent);
        }
    }

    @Override
    public boolean hidden(final @NonNull UUID uuid) {
        return this.hidden(getPlayer(uuid));
    }

    public boolean hidden(final @NonNull Player player) {
        return this.tempHidden.contains(player.getUniqueId()) ||
            pdc(player).getOrDefault(key, PersistentDataType.BYTE, (byte) 0) != (byte) 0;
    }

    private static Player getPlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            throw new IllegalArgumentException("Player is not online");
        }
        return player;
    }

    private static PersistentDataContainer pdc(Player player) {
        return player.getPersistentDataContainer();
    }

    @Override
    public boolean hidden(final @NonNull ServerPlayer player) {
        return this.hidden(player.getBukkitEntity());
    }

    @Override
    public boolean otherwiseHidden(final @NonNull ServerPlayer player) {
        return player.getBukkitEntity().hasMetadata("NPC");
    }

    @Override
    public @NonNull Component displayName(final @NonNull ServerPlayer player) {
        return player.getBukkitEntity().displayName();
    }

    @Override
    public @Nullable ServerPlayer player(final @NonNull UUID uuid) {
        final @Nullable Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return null;
        }
        return CraftBukkitReflection.serverPlayer(player);
    }
}
