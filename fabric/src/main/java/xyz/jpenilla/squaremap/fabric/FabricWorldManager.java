package xyz.jpenilla.squaremap.fabric;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.WorldManager;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.fabric.data.FabricMapWorld;

public final class FabricWorldManager implements WorldManager {
    private final Map<WorldIdentifier, FabricMapWorld> worlds = new ConcurrentHashMap<>();

    @Override
    public @NonNull Map<WorldIdentifier, MapWorldInternal> worlds() {
        return Collections.unmodifiableMap(this.worlds);
    }

    public @NonNull Optional<MapWorldInternal> getWorldIfEnabled(final @NonNull ServerLevel world) {
        if (WorldConfig.get(world).MAP_ENABLED) {
            return Optional.of(this.getWorld(world));
        } else {
            return Optional.empty();
        }
    }

    public @NonNull FabricMapWorld getWorld(final @NonNull ServerLevel world) {
        return this.worlds.computeIfAbsent(
            Util.worldIdentifier(world),
            $ -> new FabricMapWorld(world)
        );
    }

    public void start(final MinecraftServer server) {
        for (final ServerLevel level : server.getAllLevels()) {
            WorldConfig config = WorldConfig.get(level);
            if (config.MAP_ENABLED) {
                this.getWorld(level);
            }
        }
    }

    public void worldUnloaded(final @NonNull ServerLevel world) {
        this.getWorldIfEnabled(world).ifPresent(MapWorldInternal::shutdown);
        this.worlds.remove(Util.worldIdentifier(world));
    }

    public void shutdown() {
        this.worlds.values().forEach(MapWorldInternal::shutdown);
        this.worlds.clear();
    }
}
