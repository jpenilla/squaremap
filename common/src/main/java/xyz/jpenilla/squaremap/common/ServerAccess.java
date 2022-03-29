package xyz.jpenilla.squaremap.common;

import java.util.Collection;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

@DefaultQualifier(NonNull.class)
public interface ServerAccess {
    Collection<ServerLevel> levels();

    @Nullable ServerLevel level(WorldIdentifier identifier);

    @Nullable ServerPlayer player(UUID uuid);

    int maxPlayers();
}
