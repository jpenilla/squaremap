package xyz.jpenilla.squaremap.paper;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.inject.PlatformModule;
import xyz.jpenilla.squaremap.common.task.UpdatePlayers;
import xyz.jpenilla.squaremap.common.task.UpdateWorldData;
import xyz.jpenilla.squaremap.paper.inject.PaperModule;
import xyz.jpenilla.squaremap.paper.listener.MapUpdateListeners;
import xyz.jpenilla.squaremap.paper.listener.PlayerListener;
import xyz.jpenilla.squaremap.paper.listener.WorldLoadListener;
import xyz.jpenilla.squaremap.paper.network.PaperNetworking;
import xyz.jpenilla.squaremap.paper.util.BukkitRunnableAdapter;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

public final class SquaremapPlugin extends JavaPlugin implements SquaremapPlatform {
    private static final Logger LOGGER = LogManager.getLogger("squaremap");
    private static SquaremapPlugin INSTANCE;
    private SquaremapCommon common;
    private PaperWorldManager worldManager;
    private PaperPlayerManager playerManager;
    private BukkitRunnable updateWorldData;
    private BukkitRunnable updatePlayers;
    private MapUpdateListeners mapUpdateListeners;
    private WorldLoadListener worldLoadListener;
    private Injector injector;

    public SquaremapPlugin() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
        } catch (ClassNotFoundException e) {
            this.getLogger().severe("squaremap requires Paper or one of its forks to run. Get Paper from https://papermc.io/downloads");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.injector = Guice.createInjector(
            new PaperModule(this),
            new PlatformModule(this)
        );

        this.common = this.injector.getInstance(SquaremapCommon.class);
        this.getServer().getServicesManager().register(Squaremap.class, this.common.api(), this, ServicePriority.Normal);

        PaperNetworking.register(this);

        this.getServer().getPluginManager().registerEvents(this.injector.getInstance(PlayerListener.class), this);

        new Metrics(this, 13571); // https://bstats.org/plugin/bukkit/squaremap/13571

        this.getServer().getScheduler().runTask(this, this.common::updateCheck);
    }

    @Override
    public void onDisable() {
        PaperNetworking.unregister(this);
        if (this.common != null) {
            this.getServer().getServicesManager().unregister(Squaremap.class, this.common.api());
            this.common.shutdown();
        }
    }

    @Override
    public @NonNull Injector injector() {
        return this.injector;
    }

    public static SquaremapPlugin getInstance() {
        return INSTANCE;
    }

    @Override
    public void startCallback() {
        this.playerManager = this.injector.getInstance(PaperPlayerManager.class);

        this.updatePlayers = new BukkitRunnableAdapter(this.injector.getInstance(UpdatePlayers.class));
        this.updatePlayers.runTaskTimer(this, 20, 20);

        this.updateWorldData = new BukkitRunnableAdapter(this.injector.getInstance(UpdateWorldData.class));
        this.updateWorldData.runTaskTimer(this, 0, 20 * 5);

        this.worldManager = this.injector.getInstance(PaperWorldManager.class);
        this.worldManager.start(this);

        this.mapUpdateListeners = this.injector.getInstance(MapUpdateListeners.class);
        this.mapUpdateListeners.register();

        this.worldLoadListener = new WorldLoadListener(this);
        this.getServer().getPluginManager().registerEvents(this.worldLoadListener, this);
    }

    @Override
    public void stopCallback() {
        if (this.mapUpdateListeners != null) {
            this.mapUpdateListeners.unregister();
            this.mapUpdateListeners = null;
        }

        if (this.worldLoadListener != null) {
            HandlerList.unregisterAll(this.worldLoadListener);
            this.worldLoadListener = null;
        }

        if (this.updatePlayers != null) {
            if (!this.updatePlayers.isCancelled()) {
                this.updatePlayers.cancel();
            }
            this.updatePlayers = null;
        }

        if (this.updateWorldData != null) {
            if (!this.updateWorldData.isCancelled()) {
                this.updateWorldData.cancel();
            }
            this.updateWorldData = null;
        }

        if (this.worldManager != null) {
            this.worldManager.shutdown();
            this.worldManager = null;
        }

        if (this.playerManager != null) {
            this.playerManager = null;
        }

        this.getServer().getScheduler().cancelTasks(this);
    }

    @Override
    public int maxPlayers() {
        return Bukkit.getMaxPlayers();
    }

    @Override
    public @NonNull String version() {
        return this.getDescription().getVersion();
    }

    @Override
    public @NonNull PaperWorldManager worldManager() {
        return this.worldManager;
    }

    @Override
    public @NonNull PaperPlayerManager playerManager() {
        return this.playerManager;
    }

    @Override
    public @NonNull Path dataDirectory() {
        return this.getDataFolder().toPath();
    }

    @Override
    public @NonNull Logger logger() {
        return LOGGER;
    }

    @Override
    public @NonNull Collection<ServerLevel> levels() {
        final List<ServerLevel> levels = new ArrayList<>();
        for (final World world : Bukkit.getWorlds()) {
            levels.add(CraftBukkitReflection.serverLevel(world));
        }
        return levels;
    }

    @Override
    public @Nullable ServerLevel level(final @NonNull WorldIdentifier identifier) {
        final @Nullable World world = Bukkit.getWorld(BukkitAdapter.namespacedKey(identifier));
        if (world == null) {
            return null;
        }
        return CraftBukkitReflection.serverLevel(world);
    }

    @Override
    public @NonNull Path regionFileDirectory(final @NonNull ServerLevel level) {
        return LevelStorageSource.getStorageFolder(CraftBukkitReflection.world(level).getWorldFolder().toPath(), level.getTypeKey()).resolve("region");
    }
}
