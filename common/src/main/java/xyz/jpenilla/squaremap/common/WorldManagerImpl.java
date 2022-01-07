package xyz.jpenilla.squaremap.common;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.Util;

public class WorldManagerImpl<W extends MapWorldInternal> implements WorldManager {
    private final Map<WorldIdentifier, W> worlds = new ConcurrentHashMap<>();
    private final Function<ServerLevel, W> factory;

    public WorldManagerImpl(
        final Function<ServerLevel, W> factory
    ) {
        this.factory = factory;
    }

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

    private @NonNull W create(final @NonNull ServerLevel world) {
        return this.factory.apply(world);
    }

    public @NonNull W getWorld(final @NonNull ServerLevel world) {
        return this.worlds.computeIfAbsent(
            Util.worldIdentifier(world),
            $ -> this.create(world)
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
