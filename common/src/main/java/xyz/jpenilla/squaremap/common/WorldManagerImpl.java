package xyz.jpenilla.squaremap.common;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public class WorldManagerImpl<W extends MapWorldInternal> implements WorldManager {
    private final Map<WorldIdentifier, W> worlds = new ConcurrentHashMap<>();
    private final Function<ServerLevel, W> factory;

    public WorldManagerImpl(
        final Function<ServerLevel, W> factory
    ) {
        this.factory = factory;
    }

    @Override
    public Map<WorldIdentifier, MapWorldInternal> worlds() {
        return Collections.unmodifiableMap(this.worlds);
    }

    public Optional<MapWorldInternal> getWorldIfEnabled(final ServerLevel world) {
        if (WorldConfig.get(world).MAP_ENABLED) {
            return Optional.of(this.getWorld(world));
        } else {
            return Optional.empty();
        }
    }

    private W create(final ServerLevel world) {
        return this.factory.apply(world);
    }

    public W getWorld(final ServerLevel world) {
        return this.worlds.computeIfAbsent(
            Util.worldIdentifier(world),
            $ -> this.create(world)
        );
    }

    public void start(final SquaremapPlatform platform) {
        for (final ServerLevel level : platform.levels()) {
            this.getWorldIfEnabled(level);
        }
    }

    public void worldUnloaded(final ServerLevel world) {
        this.getWorldIfEnabled(world).ifPresent(MapWorldInternal::shutdown);
        this.worlds.remove(Util.worldIdentifier(world));
    }

    public void shutdown() {
        this.worlds.values().forEach(MapWorldInternal::shutdown);
        this.worlds.clear();
    }
}
