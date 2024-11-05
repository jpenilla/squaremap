package xyz.jpenilla.squaremap.forge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.util.SleepBlockingMinecraftServer;
import xyz.jpenilla.squaremap.common.ServerAccess;

@DefaultQualifier(NonNull.class)
@Singleton
public final class ForgeServerAccess implements ServerAccess {
    private @Nullable MinecraftServer server;

    @Inject
    private ForgeServerAccess() {
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
    public @Nullable ServerPlayer player(final UUID uuid) {
        return this.requireServer().getPlayerList().getPlayer(uuid);
    }

    @Override
    public int maxPlayers() {
        return this.requireServer().getMaxPlayers();
    }

    @Override
    public void blockSleep() {
        ((SleepBlockingMinecraftServer) this.requireServer()).squaremap$blockSleep();
    }

    @Override
    public void allowSleep() {
        ((SleepBlockingMinecraftServer) this.requireServer()).squaremap$allowSleep();
    }
}
