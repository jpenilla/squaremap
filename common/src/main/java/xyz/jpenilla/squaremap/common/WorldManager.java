package xyz.jpenilla.squaremap.common;

import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;

@DefaultQualifier(NonNull.class)
public interface WorldManager {
    Map<WorldIdentifier, MapWorldInternal> worlds();

    Optional<MapWorldInternal> getWorldIfEnabled(ServerLevel level);
}
