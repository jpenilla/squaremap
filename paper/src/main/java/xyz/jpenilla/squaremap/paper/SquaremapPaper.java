package xyz.jpenilla.squaremap.paper;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.papermc.paper.threadedregions.RegionizedServerInitEvent;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.bstats.bukkit.Metrics;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.task.UpdatePlayers;
import xyz.jpenilla.squaremap.common.task.UpdateWorldData;
import xyz.jpenilla.squaremap.common.util.ExceptionLoggingScheduledThreadPoolExecutor;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.paper.listener.MapUpdateListeners;
import xyz.jpenilla.squaremap.paper.listener.WorldLoadListener;
import xyz.jpenilla.squaremap.paper.network.PaperNetworking;
import xyz.jpenilla.squaremap.paper.util.Folia;

@DefaultQualifier(NonNull.class)
@Singleton
public final class SquaremapPaper implements SquaremapPlatform {
    private final Injector injector;
    private final SquaremapCommon common;
    private final PaperNetworking networking;
    private final JavaPlugin plugin;
    private final Server server;
    private @MonotonicNonNull Squaremap api;
    private @Nullable ScheduledFuture<?> updateWorldData;
    private @Nullable ScheduledFuture<?> updatePlayers;
    private @Nullable MapUpdateListeners mapUpdateListeners;
    private @Nullable WorldLoadListener worldLoadListener;
    private @Nullable ScheduledExecutorService taskPool;

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
        if (Folia.FOLIA) {
            this.server.getPluginManager().registerEvents(new FoliaInitListener(), this.plugin);
        } else {
            this.server.getScheduler().runTask(this.plugin, this.common::updateCheck);
        }
    }

    public final class FoliaInitListener implements Listener {
        @EventHandler
        public void handle(final RegionizedServerInitEvent event) {
            SquaremapPaper.this.server.getAsyncScheduler().runNow(SquaremapPaper.this.plugin, $ -> SquaremapPaper.this.common.updateCheck());
        }
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

        this.taskPool = new ExceptionLoggingScheduledThreadPoolExecutor(1, Util.squaremapThreadFactory("tasks"));

        this.updatePlayers = this.taskPool.scheduleAtFixedRate(
            this.injector.getInstance(UpdatePlayers.class),
            1,
            1,
            TimeUnit.SECONDS
        );
        this.updateWorldData = this.taskPool.scheduleAtFixedRate(
            this.injector.getInstance(UpdateWorldData.class),
            0,
            5,
            TimeUnit.SECONDS
        );
    }

    @Override
    public void stopCallback() {
        if (this.updateWorldData != null) {
            if (!this.updateWorldData.isCancelled()) {
                this.updateWorldData.cancel(false);
            }
            this.updateWorldData = null;
        }

        if (this.updatePlayers != null) {
            if (!this.updatePlayers.isCancelled()) {
                this.updatePlayers.cancel(false);
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

        if (this.taskPool != null) {
            Util.shutdownExecutor(this.taskPool, TimeUnit.MILLISECONDS, 500);
        }
    }

    @Override
    public @NonNull String version() {
        return this.plugin.getDescription().getVersion();
    }
}
