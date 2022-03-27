package xyz.jpenilla.squaremap.common;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public abstract class AbstractWorldManager<W extends MapWorldInternal> implements WorldManager {
    private final Map<WorldIdentifier, W> worlds = new ConcurrentHashMap<>();
    private final MapWorldInternal.Factory<W> factory;
    private final ServerAccess serverAccess;

    protected AbstractWorldManager(
        final MapWorldInternal.Factory<W> factory,
        final ServerAccess serverAccess
    ) {
        this.factory = factory;
        this.serverAccess = serverAccess;
    }

    @Override
    public Map<WorldIdentifier, MapWorldInternal> worlds() {
        return Collections.unmodifiableMap(this.worlds);
    }

    @Override
    public Optional<MapWorldInternal> getWorldIfEnabled(final ServerLevel world) {
        if (WorldConfig.get(world).MAP_ENABLED) {
            return Optional.of(this.getOrCreateMapWorld(world));
        } else {
            return Optional.empty();
        }
    }

    private W getOrCreateMapWorld(final ServerLevel world) {
        return this.worlds.computeIfAbsent(
            Util.worldIdentifier(world),
            $ -> this.factory.create(world)
        );
    }

    public void start() {
        for (final ServerLevel level : this.serverAccess.levels()) {
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
