package xyz.jpenilla.squaremap.common;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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
    public Collection<MapWorldInternal> worlds() {
        return Collections.unmodifiableCollection(this.worlds.values());
    }

    @Override
    public Optional<MapWorldInternal> getWorldIfEnabled(final WorldIdentifier worldIdentifier) {
        return Optional.ofNullable(this.worlds.get(worldIdentifier));
    }

    @Override
    public Optional<MapWorldInternal> getWorldIfEnabled(final ServerLevel level) {
        if (WorldConfig.get(level).MAP_ENABLED) {
            return Optional.of(this.getOrCreateMapWorld(level));
        }
        return Optional.empty();
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
        final @Nullable W removed = this.worlds.remove(Util.worldIdentifier(world));
        if (removed != null) {
            tryShutdown(removed);
        }
    }

    public void shutdown() {
        final List<W> worlds = List.copyOf(this.worlds.values());
        this.worlds.clear();
        for (final W world : worlds) {
            tryShutdown(world);
        }
    }

    private static void tryShutdown(final MapWorldInternal mapWorld) {
        try {
            mapWorld.shutdown();
        } catch (final Exception ex) {
            Logging.logger().error("Exception shutting down map world '{}'", mapWorld.identifier().asString(), ex);
        }
    }
}
