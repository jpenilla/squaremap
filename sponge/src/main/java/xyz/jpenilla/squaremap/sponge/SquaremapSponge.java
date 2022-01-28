package xyz.jpenilla.squaremap.sponge;

import cloud.commandframework.CommandManager;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.server.level.ServerLevel;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.WorldManagerImpl;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.task.UpdatePlayers;
import xyz.jpenilla.squaremap.common.task.UpdateWorldData;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.common.util.VanillaChunkSnapshotProvider;
import xyz.jpenilla.squaremap.sponge.command.SpongeCommands;
import xyz.jpenilla.squaremap.sponge.data.SpongeMapWorld;
import xyz.jpenilla.squaremap.sponge.listener.MapUpdateListener;
import xyz.jpenilla.squaremap.sponge.listener.WorldLoadListener;
import xyz.jpenilla.squaremap.sponge.network.SpongeNetwork;

@DefaultQualifier(NonNull.class)
@Plugin("squaremap")
public final class SquaremapSponge implements SquaremapPlatform {
    private final Path dataDirectory;
    private final PluginContainer pluginContainer;
    private final Game game;
    private final SquaremapCommon common;
    private @Nullable MapUpdateListener mapUpdateListener;
    private @Nullable WorldManagerImpl<SpongeMapWorld> worldManager;
    private @Nullable SpongePlayerManager playerManager;
    private @Nullable ScheduledTask updateWorlds;
    private @Nullable ScheduledTask updatePlayers;
    private @Nullable WorldLoadListener worldLoadListener;

    @Inject
    public SquaremapSponge(
        @ConfigDir(sharedRoot = false) final Path dataDirectory,
        final PluginContainer pluginContainer,
        final Game game
    ) {
        this.dataDirectory = dataDirectory;
        this.pluginContainer = pluginContainer;
        this.game = game;
        this.common = new SquaremapCommon(this);
        SpongeCommands.register(this.common);
        this.game.eventManager().registerListeners(this.pluginContainer, new SpongeNetwork());
    }

    @Listener
    public void registerData(final RegisterDataEvent event) {
        event.register(DataRegistration.of(SpongePlayerManager.HIDDEN_KEY, ServerPlayer.class));
    }

    @Listener
    public void gameLoaded(final StartedEngineEvent<Server> event) {
        this.scheduleTasks();
    }

    @Listener
    public void shutdown(final StoppingEngineEvent<Server> event) {
        this.common.shutdown();
    }

    @Override
    public void startCallback() {
        this.worldManager = new WorldManagerImpl<>(SpongeMapWorld::new);
        this.worldManager.start(this);

        this.playerManager = new SpongePlayerManager();

        if (this.game.isServerAvailable()) {
            this.scheduleTasks();
        }

        this.mapUpdateListener = new MapUpdateListener(this);
        this.mapUpdateListener.register();

        this.worldLoadListener = new WorldLoadListener(this);
        this.game.eventManager().registerListeners(this.pluginContainer, this.worldLoadListener);
    }

    @Override
    public void stopCallback() {
        if (this.updatePlayers != null) {
            this.updatePlayers.cancel();
            this.updatePlayers = null;
        }

        if (this.updateWorlds != null) {
            this.updateWorlds.cancel();
            this.updateWorlds = null;
        }

        if (this.worldManager != null) {
            this.worldManager.shutdown();
            this.worldManager = null;
        }

        this.playerManager = null;

        if (this.mapUpdateListener != null) {
            this.mapUpdateListener.unregister();
            this.mapUpdateListener = null;
        }

        if (this.worldLoadListener != null) {
            this.game.eventManager().unregisterListeners(this.worldLoadListener);
            this.worldLoadListener = null;
        }
    }

    @Listener
    public void reload(final RefreshGameEvent event) {
        // try to find the right audience, while also logging to console

        if (event.cause().containsType(SystemSubject.class)) {
            this.common.reload(this.game.systemSubject());
            return;
        }

        final List<Audience> audiences = new ArrayList<>(event.cause().allOf(Audience.class));
        final Audience audience;
        if (audiences.isEmpty()) {
            audience = this.game.systemSubject();
        } else {
            audiences.add(this.game.systemSubject());
            audience = Audience.audience(audiences);
        }
        this.common.reload(audience);
    }

    public PluginContainer pluginContainer() {
        return this.pluginContainer;
    }

    @Override
    public ChunkSnapshotProvider chunkSnapshotProvider() {
        return VanillaChunkSnapshotProvider.get();
    }

    @Override
    public WorldManagerImpl<SpongeMapWorld> worldManager() {
        return this.worldManager;
    }

    @Override
    public Path dataDirectory() {
        return this.dataDirectory;
    }

    @Override
    public Logger logger() {
        return this.pluginContainer.logger();
    }

    @Override
    public ComponentFlattener componentFlattener() {
        final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
        final Field flattener = ReflectionUtil.needField(plainText.getClass(), "flattener");
        try {
            return (ComponentFlattener) flattener.get(plainText);
        } catch (final IllegalAccessException ex) {
            throw Util.rethrow(ex);
        }
    }

    @Override
    public Collection<ServerLevel> levels() {
        if (!this.game.isServerAvailable()) {
            return List.of();
        }
        return this.game.server().worldManager().worlds().stream()
            .map(level -> (ServerLevel) level)
            .toList();
    }

    @Override
    public @Nullable ServerLevel level(final WorldIdentifier identifier) {
        return (ServerLevel) this.game.server().worldManager()
            .world(ResourceKey.of(identifier.namespace(), identifier.value()))
            .orElse(null);
    }

    @Override
    public SpongePlayerManager playerManager() {
        return this.playerManager;
    }

    private void scheduleTasks() {
        this.updateWorlds = this.game.server().scheduler().submit(
            Task.builder()
                .plugin(this.pluginContainer)
                .interval(Duration.ofSeconds(5))
                .execute(new UpdateWorldData())
                .build()
        );
        this.updatePlayers = this.game.server().scheduler().submit(
            Task.builder()
                .plugin(this.pluginContainer)
                .interval(Duration.ofSeconds(1))
                .execute(new UpdatePlayers(this))
                .build()
        );
    }

    @Override
    public int maxPlayers() {
        return this.game.server().maxPlayers();
    }

    @Override
    public String version() {
        return this.pluginContainer.metadata().version().toString();
    }

    @Override
    public CommandManager<Commander> createCommandManager() {
        return SpongeCommands.createCommandManager(this);
    }

    public Game game() {
        return this.game;
    }
}
