package xyz.jpenilla.squaremap.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.EmptySectionHolder;

@DefaultQualifier(NonNull.class)
@Singleton
public class WorldManagerImpl implements WorldManager {
    private final Map<WorldIdentifier, MapWorldInternal> worlds = new ConcurrentHashMap<>();
    private final MapWorldInternal.Factory factory;
    protected final ServerAccess serverAccess;
    private final ConfigManager configManager;

    @Inject
    protected WorldManagerImpl(
        final MapWorldInternal.Factory factory,
        final ServerAccess serverAccess,
        final ConfigManager configManager
    ) {
        this.factory = factory;
        this.serverAccess = serverAccess;
        this.configManager = configManager;
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
        return this.getWorldIfEnabled(Util.worldIdentifier(level));
    }

    public void initWorld(final ServerLevel level) {
        EmptySectionHolder.init(level.palettedContainerFactory());
        final WorldIdentifier identifier = Util.worldIdentifier(level);
        if (this.worlds.containsKey(identifier)) {
            throw new IllegalStateException("MapWorld already exists for '" + identifier.asString() + "'");
        }
        if (this.configManager.worldConfig(level).MAP_ENABLED) {
            this.worlds.put(identifier, this.factory.create(level));
        }
    }

    public void start() {
        for (final ServerLevel level : this.serverAccess.levels()) {
            this.initWorld(level);
        }
    }

    public void worldUnloaded(final ServerLevel world) {
        final @Nullable MapWorldInternal removed = this.worlds.remove(Util.worldIdentifier(world));
        if (removed != null) {
            tryShutdown(removed);
        }
    }

    public void shutdown() {
        final List<MapWorldInternal> worlds = List.copyOf(this.worlds.values());
        this.worlds.clear();
        for (final MapWorldInternal world : worlds) {
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
