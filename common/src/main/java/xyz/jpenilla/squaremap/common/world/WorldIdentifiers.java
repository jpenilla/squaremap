package xyz.jpenilla.squaremap.common.world;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

@DefaultQualifier(NonNull.class)
public final class WorldIdentifiers {
    private WorldIdentifiers() {
    }

    public static String configName(final ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    public static String webName(final ServerLevel level) {
        return level.dimension().identifier().toString().replace(":", "_");
    }

    public static WorldIdentifier identifier(final ServerLevel level) {
        final Identifier location = level.dimension().identifier();
        return identifier(location);
    }

    public static WorldIdentifier identifier(final Identifier location) {
        return WorldIdentifier.create(location.getNamespace(), location.getPath());
    }
}
