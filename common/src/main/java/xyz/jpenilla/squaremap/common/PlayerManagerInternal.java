package xyz.jpenilla.squaremap.common;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.PlayerManager;

@DefaultQualifier(NonNull.class)
public interface PlayerManagerInternal extends PlayerManager {
    boolean hidden(ServerPlayer player);

    default boolean otherwiseHidden(ServerPlayer player) {
        return false;
    }

    Component displayName(ServerPlayer player);

    @Nullable ServerPlayer player(UUID uuid);
}
