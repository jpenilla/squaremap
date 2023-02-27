package xyz.jpenilla.squaremap.fabric;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;
import xyz.jpenilla.squaremap.common.ServerAccess;

@DefaultQualifier(NonNull.class)
@Singleton
public final class FabricPlayerManager extends AbstractPlayerManager {
    @Inject
    private FabricPlayerManager(final ServerAccess serverAccess) {
        super(serverAccess);
    }

    @Override
    public Component displayName(final ServerPlayer player) {
        return player.getDisplayName().asComponent();
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
        return player.getComponent(SquaremapComponentInitializer.SQUAREMAP_PLAYER_COMPONENT);
    }
}
