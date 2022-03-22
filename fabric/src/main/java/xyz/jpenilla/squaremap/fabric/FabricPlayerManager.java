package xyz.jpenilla.squaremap.fabric;

import com.google.inject.Inject;
import java.util.UUID;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;

@DefaultQualifier(NonNull.class)
public final class FabricPlayerManager extends AbstractPlayerManager {
    private final FabricServerAccess serverAccess;

    @Inject
    private FabricPlayerManager(final FabricServerAccess serverAccess) {
        this.serverAccess = serverAccess;
    }

    @Override
    public Component displayName(final ServerPlayer player) {
        return FabricServerAudiences.of(player.getServer()).toAdventure(player.getDisplayName());
    }

    @Override
    public @Nullable ServerPlayer player(final UUID uuid) {
        return this.serverAccess.requireServer().getPlayerList().getPlayer(uuid);
    }

    @Override
    protected boolean persistentHidden(final ServerPlayer player) {
        return component(player).hidden();
    }

    @Override
    protected void persistentHidden(final ServerPlayer player, final boolean value) {
        component(player).hidden(value);
    }

    private static SquaremapComponentInitializer.PlayerComponent component(final ServerPlayer player) {
        return SquaremapComponentInitializer.SQUAREMAP_PLAYER_COMPONENT.get(player);
    }
}
