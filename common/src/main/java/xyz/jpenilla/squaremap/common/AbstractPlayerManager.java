package xyz.jpenilla.squaremap.common;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.PlayerManager;

@DefaultQualifier(NonNull.class)
public abstract class AbstractPlayerManager implements PlayerManager {
    private final ServerAccess serverAccess;
    private final Set<UUID> hidden = ConcurrentHashMap.newKeySet();

    protected AbstractPlayerManager(final ServerAccess serverAccess) {
        this.serverAccess = serverAccess;
    }

    public boolean otherwiseHidden(final ServerPlayer player) {
        return false;
    }

    public abstract Component displayName(ServerPlayer player);

    protected abstract boolean persistentHidden(ServerPlayer player);

    protected abstract void persistentHidden(ServerPlayer player, boolean hide);

    @Override
    public final void hide(final UUID uuid, boolean persistent) {
        this.hide(requirePlayer(uuid), persistent);
    }

    public final void hide(final ServerPlayer player, boolean persistent) {
        if (persistent) {
            this.persistentHidden(player, true);
        }
        this.hidden.add(player.getUUID());
    }

    @Override
    public final void show(final UUID uuid, boolean persistent) {
        this.show(requirePlayer(uuid), persistent);
    }

    public final void show(final ServerPlayer player, boolean persistent) {
        if (persistent) {
            this.persistentHidden(player, false);
        }
        this.hidden.remove(player.getUUID());
    }

    @Override
    public final void hidden(final UUID uuid, final boolean hide, boolean persistent) {
        this.hidden(requirePlayer(uuid), hide, persistent);
    }

    @Override
    public final boolean hidden(final UUID uuid) {
        return this.hidden(requirePlayer(uuid));
    }

    public final void reload() {
        this.hidden.clear(); // API Javadoc states non-persistent hidden status will be cleared on plugin reload
    }

    public final boolean hidden(final ServerPlayer player) {
        return this.hidden.contains(player.getUUID())
            || this.persistentHidden(player);
    }

    public final void hidden(final ServerPlayer player, final boolean hide, boolean persistent) {
        if (hide) {
            this.hide(player, persistent);
        } else {
            this.show(player, persistent);
        }
    }

    private ServerPlayer requirePlayer(final UUID uuid) {
        final @Nullable ServerPlayer player = this.serverAccess.player(uuid);
        if (player == null) {
            throw new IllegalArgumentException("Player with uuid '" + uuid + "' is not online.");
        }
        return player;
    }
}
