package xyz.jpenilla.squaremap.common;

import java.util.Collection;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

public interface ServerAccess {
    Collection<ServerLevel> levels();

    @Nullable ServerLevel level(WorldIdentifier identifier);

    int maxPlayers();
}
