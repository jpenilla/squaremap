package xyz.jpenilla.squaremap.fabric;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.common.PlayerManagerInternal;
import xyz.jpenilla.squaremap.common.SquaremapCommon;

public final class FabricPlayerManager implements PlayerManagerInternal {
    private final Set<UUID> tempHidden = ConcurrentHashMap.newKeySet();

    @Override
    public void hide(final @NonNull UUID uuid, boolean persistent) {
        this.hide(getPlayer(uuid), persistent);
    }

    public void hide(final @NonNull ServerPlayer player, boolean persistent) {
        if (persistent) {
            //pdc(player).set(key, PersistentDataType.BYTE, (byte) 1);
        }
        this.tempHidden.add(player.getUUID());
    }

    @Override
    public void show(final @NonNull UUID uuid, boolean persistent) {
        this.show(getPlayer(uuid), persistent);
    }

    public void show(final @NonNull ServerPlayer player, boolean persistent) {
        if (persistent) {
            //pdc(player).set(key, PersistentDataType.BYTE, (byte) 0);
        }
        this.tempHidden.remove(player.getUUID());
    }

    @Override
    public void hidden(@NonNull UUID uuid, final boolean hide, boolean persistent) {
        this.hidden(getPlayer(uuid), hide, persistent);
    }

    public void hidden(@NonNull ServerPlayer player, final boolean hide, boolean persistent) {
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

    private static ServerPlayer getPlayer(final @NonNull UUID uuid) {
        final @Nullable ServerPlayer player = SquaremapCommon.instance().platform().playerManager().player(uuid);
        if (player == null) {
            throw new IllegalArgumentException("Player is not online");
        }
        return player;
    }

    /*
    private static PersistentDataContainer pdc(Player player) {
        return player.getPersistentDataContainer();
    }
     */

    @Override
    public boolean hidden(final @NonNull ServerPlayer player) {
        return this.tempHidden.contains(player.getUUID());
        // || pdc(player).getOrDefault(key, PersistentDataType.BYTE, (byte) 0) != (byte) 0;
    }

    @Override
    public @NonNull Component displayName(final @NonNull ServerPlayer player) {
        return FabricServerAudiences.of(player.getServer()).toAdventure(player.getDisplayName());
    }

    @Override
    public @Nullable ServerPlayer player(final @NonNull UUID uuid) {
        return ((SquaremapFabricInitializer) SquaremapCommon.instance().platform()).server().getPlayerList().getPlayer(uuid);
    }
}
