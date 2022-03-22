package xyz.jpenilla.squaremap.fabric;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.ServerAccess;

@DefaultQualifier(NonNull.class)
@Singleton
public final class FabricServerAccess implements ServerAccess {
    private @Nullable MinecraftServer server;

    @Inject
    private FabricServerAccess() {
    }

    public void setServer(final MinecraftServer server) {
        this.server = server;
    }

    public void clearServer() {
        this.server = null;
    }

    public MinecraftServer requireServer() {
        if (this.server == null) {
            throw new IllegalStateException("MinecraftServer was requested when not active");
        }
        return this.server;
    }

    @Override
    public Collection<ServerLevel> levels() {
        if (this.server == null) {
            return List.of();
        }
        final List<ServerLevel> levels = new ArrayList<>();
        for (final ServerLevel level : this.server.getAllLevels()) {
            levels.add(level);
        }
        return Collections.unmodifiableList(levels);
    }

    @Override
    public @Nullable ServerLevel level(final WorldIdentifier identifier) {
        if (this.server == null) {
            return null;
        }
        for (final ServerLevel level : this.server.getAllLevels()) {
            if (level.dimension().location().getNamespace().equals(identifier.namespace())
                && level.dimension().location().getPath().equals(identifier.value())) {
                return level;
            }
        }
        return null;
    }

    @Override
    public int maxPlayers() {
        return this.requireServer().getMaxPlayers();
    }
}
