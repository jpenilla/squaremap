package xyz.jpenilla.squaremap.sponge;

import com.google.inject.Inject;
import com.google.inject.Injector;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.task.UpdatePlayers;
import xyz.jpenilla.squaremap.common.task.UpdateWorldData;
import xyz.jpenilla.squaremap.sponge.listener.MapUpdateListener;
import xyz.jpenilla.squaremap.sponge.listener.WorldLoadListener;
import xyz.jpenilla.squaremap.sponge.network.SpongeNetworking;

@DefaultQualifier(NonNull.class)
public final class SquaremapSponge implements SquaremapPlatform {
    private final PluginContainer pluginContainer;
    private final Game game;
    private final SquaremapCommon common;
    private final Injector injector;
    private @Nullable MapUpdateListener mapUpdateListener;
    private @Nullable ScheduledTask updateWorlds;
    private @Nullable ScheduledTask updatePlayers;
    private @Nullable WorldLoadListener worldLoadListener;

    @Inject
    public SquaremapSponge(
        final PluginContainer pluginContainer,
        final Game game,
        final Injector injector
    ) {
        game.eventManager().registerListeners(pluginContainer, this, MethodHandles.lookup());
        this.pluginContainer = pluginContainer;
        this.game = game;
        this.injector = injector;
        this.common = injector.getInstance(SquaremapCommon.class);
        this.game.eventManager().registerListeners(this.pluginContainer, injector.getInstance(SpongeNetworking.class), MethodHandles.lookup());
    }

    void init() {
        this.common.init();
    }

    @Listener
    public void gameLoaded(final StartedEngineEvent<Server> event) {
        this.scheduleTasks();
        this.game.server().scheduler().submit(
            Task.builder()
                .plugin(this.pluginContainer)
                .execute(this.common::updateCheck)
                .build()
        );
    }

    @Listener
    public void shutdown(final StoppingEngineEvent<Server> event) {
        this.common.shutdown();
    }

    @Override
    public void startCallback() {
        this.worldLoadListener = this.injector.getInstance(WorldLoadListener.class);
        this.game.eventManager().registerListeners(this.pluginContainer, this.worldLoadListener, MethodHandles.lookup());

        this.mapUpdateListener = this.injector.getInstance(MapUpdateListener.class);
        this.mapUpdateListener.register();

        if (this.game.isServerAvailable()) {
            this.scheduleTasks();
        }
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

    private void scheduleTasks() {
        this.updateWorlds = this.game.server().scheduler().submit(
            Task.builder()
                .plugin(this.pluginContainer)
                .interval(Duration.ofSeconds(5))
                .execute(this.injector.getInstance(UpdateWorldData.class))
                .build()
        );
        this.updatePlayers = this.game.server().scheduler().submit(
            Task.builder()
                .plugin(this.pluginContainer)
                .interval(Duration.ofSeconds(1))
                .execute(this.injector.getInstance(UpdatePlayers.class))
                .build()
        );
    }

    @Override
    public String version() {
        return this.pluginContainer.metadata().version().toString();
    }
}
