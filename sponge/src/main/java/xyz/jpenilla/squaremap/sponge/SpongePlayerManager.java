package xyz.jpenilla.squaremap.sponge;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import xyz.jpenilla.squaremap.common.PlayerManagerInternal;
import xyz.jpenilla.squaremap.common.SquaremapCommon;

public final class SpongePlayerManager implements PlayerManagerInternal {
    private static final Key<Value<Boolean>> KEY = Key.from(ResourceKey.of("squaremap", "hidden"), Boolean.class);

    private final Set<UUID> tempHidden = ConcurrentHashMap.newKeySet();

    @Override
    public void hide(final @NonNull UUID uuid, boolean persistent) {
        this.hide(getPlayer(uuid), persistent);
    }

    public void hide(final @NonNull ServerPlayer player, boolean persistent) {
        if (persistent) {
            set((org.spongepowered.api.entity.living.player.server.ServerPlayer) player, true);
        }
        this.tempHidden.add(player.getUUID());
    }

    @Override
    public void show(final @NonNull UUID uuid, boolean persistent) {
        this.show(getPlayer(uuid), persistent);
    }

    public void show(final @NonNull ServerPlayer player, boolean persistent) {
        if (persistent) {
            set((org.spongepowered.api.entity.living.player.server.ServerPlayer) player, false);
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

    @Override
    public boolean hidden(final @NonNull ServerPlayer player) {
        return this.tempHidden.contains(player.getUUID())
            || get((org.spongepowered.api.entity.living.player.server.ServerPlayer) player);
    }

    private static void set(final org.spongepowered.api.entity.living.player.server.ServerPlayer player, final boolean bool) {
        player.offer(KEY, bool);
    }

    private static boolean get(final org.spongepowered.api.entity.living.player.server.ServerPlayer player) {
        return player.get(KEY).orElse(false);
    }

    @Override
    public @NonNull Component displayName(final @NonNull ServerPlayer player) {
        return ((org.spongepowered.api.entity.living.player.server.ServerPlayer) player).displayName().get();
    }

    @Override
    public @Nullable ServerPlayer player(final @NonNull UUID uuid) {
        return (ServerPlayer) Sponge.server().player(uuid).orElse(null);
    }
}
