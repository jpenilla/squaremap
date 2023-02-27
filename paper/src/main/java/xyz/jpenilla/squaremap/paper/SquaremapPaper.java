package xyz.jpenilla.squaremap.paper;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.bstats.bukkit.Metrics;
import org.bukkit.Server;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.task.UpdatePlayers;
import xyz.jpenilla.squaremap.common.task.UpdateWorldData;
import xyz.jpenilla.squaremap.paper.listener.MapUpdateListeners;
import xyz.jpenilla.squaremap.paper.listener.WorldLoadListener;
import xyz.jpenilla.squaremap.paper.network.PaperNetworking;

@DefaultQualifier(NonNull.class)
@Singleton
public final class SquaremapPaper implements SquaremapPlatform {
    private final Injector injector;
    private final SquaremapCommon common;
    private final PaperNetworking networking;
    private final JavaPlugin plugin;
    private final Server server;
    private @MonotonicNonNull Squaremap api;
    private @Nullable BukkitTask updateWorldData;
    private @Nullable BukkitTask updatePlayers;
    private @Nullable MapUpdateListeners mapUpdateListeners;
    private @Nullable WorldLoadListener worldLoadListener;

    @Inject
    private SquaremapPaper(
        final Injector injector,
        final SquaremapCommon squaremapCommon,
        final Server server,
        final JavaPlugin plugin,
        final PaperNetworking networking
    ) {
        this.injector = injector;
        this.common = squaremapCommon;
        this.server = server;
        this.plugin = plugin;
        this.networking = networking;
    }

    void init() {
        this.common.init();
        this.api = this.injector.getInstance(Squaremap.class);
        this.server.getServicesManager().register(Squaremap.class, this.api, this.plugin, ServicePriority.Normal);
        this.networking.register();
        new Metrics(this.plugin, 13571); // https://bstats.org/plugin/bukkit/squaremap/13571
        this.server.getScheduler().runTask(this.plugin, this.common::updateCheck);
    }

    void onDisable() {
        this.networking.unregister();
        if (this.api != null) {
            this.server.getServicesManager().unregister(Squaremap.class, this.api);
        }
        this.common.shutdown();
    }

    @Override
    public void startCallback() {
        this.worldLoadListener = this.injector.getInstance(WorldLoadListener.class);
        this.server.getPluginManager().registerEvents(this.worldLoadListener, this.plugin);

        this.mapUpdateListeners = this.injector.getInstance(MapUpdateListeners.class);
        this.mapUpdateListeners.register();

        this.updatePlayers = this.server.getScheduler()
            .runTaskTimer(this.plugin, this.injector.getInstance(UpdatePlayers.class), 20, 20);

        this.updateWorldData = this.server.getScheduler()
            .runTaskTimer(this.plugin, this.injector.getInstance(UpdateWorldData.class), 0, 20 * 5);
    }

    @Override
    public void stopCallback() {
        if (this.updateWorldData != null) {
            if (!this.updateWorldData.isCancelled()) {
                this.updateWorldData.cancel();
            }
            this.updateWorldData = null;
        }

        if (this.updatePlayers != null) {
            if (!this.updatePlayers.isCancelled()) {
                this.updatePlayers.cancel();
            }
            this.updatePlayers = null;
        }

        if (this.mapUpdateListeners != null) {
            this.mapUpdateListeners.unregister();
            this.mapUpdateListeners = null;
        }

        if (this.worldLoadListener != null) {
            HandlerList.unregisterAll(this.worldLoadListener);
            this.worldLoadListener = null;
        }

        this.server.getScheduler().cancelTasks(this.plugin);
    }

    @Override
    public @NonNull String version() {
        return this.plugin.getDescription().getVersion();
    }
}
