package xyz.jpenilla.squaremap.plugin;

import io.papermc.paper.text.PaperComponents;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.imageio.ImageIO;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.logging.log4j.Logger;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.config.Advanced;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.httpd.IntegratedServer;
import xyz.jpenilla.squaremap.common.layer.SpawnIconProvider;
import xyz.jpenilla.squaremap.common.util.BiomeSpecialEffectsAccess;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.common.util.FileUtil;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;
import xyz.jpenilla.squaremap.plugin.command.Commands;
import xyz.jpenilla.squaremap.plugin.listener.MapUpdateListeners;
import xyz.jpenilla.squaremap.plugin.listener.PlayerListener;
import xyz.jpenilla.squaremap.plugin.listener.WorldEventListener;
import xyz.jpenilla.squaremap.plugin.network.Network;
import xyz.jpenilla.squaremap.plugin.task.UpdatePlayers;
import xyz.jpenilla.squaremap.plugin.task.UpdateWorldData;
import xyz.jpenilla.squaremap.plugin.util.CraftBukkitReflection;
import xyz.jpenilla.squaremap.plugin.util.PaperChunkSnapshotProvider;

public final class SquaremapPlugin extends JavaPlugin implements SquaremapPlatform {
    private static SquaremapPlugin instance;
    private SquaremapCommon common;
    private Squaremap squaremap;
    private PaperWorldManager worldManager;
    private PaperPlayerManager playerManager;
    private UpdateWorldData updateWorldData;
    private UpdatePlayers updatePlayers;
    private MapUpdateListeners mapUpdateListeners;
    private WorldEventListener worldEventListener;

    public SquaremapPlugin() {
        instance = this;
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

        this.common = new SquaremapCommon(this);

        Config.reload();

        // this has to load after config.yml in order to know if web dir should be overwritten
        // but also before advanced.yml to ensure foliage.png and grass.png are already on disk
        FileUtil.extract("/web/", FileUtil.WEB_DIR.toFile(), Config.UPDATE_WEB_DIR);
        FileUtil.extract("/locale/", FileUtil.LOCALE_DIR.toFile(), false);

        Advanced.reload();
        Lang.reload();

        new Commands(this);

        this.start();
        this.setupApi();

        try {
            this.api().iconRegistry().register(SpawnIconProvider.SPAWN_ICON_KEY, ImageIO.read(FileUtil.WEB_DIR.resolve("images/icon/spawn.png").toFile()));
        } catch (IOException e) {
            Logging.logger().warn("Failed to register spawn icon", e);
        }

        Network.register();

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        new Metrics(this, 13571); // https://bstats.org/plugin/bukkit/squaremap/13571
    }

    @Override
    public void onDisable() {
        Network.unregister();
        this.shutdownApi();
        this.stop();
    }

    public static SquaremapPlugin getInstance() {
        return instance;
    }

    public void start() {
        this.playerManager = new PaperPlayerManager();

        this.updatePlayers = new UpdatePlayers(this);
        this.updatePlayers.runTaskTimer(this, 20, 20);

        this.updateWorldData = new UpdateWorldData();
        this.updateWorldData.runTaskTimer(this, 0, 20 * 5);

        this.worldManager = new PaperWorldManager();
        this.worldManager.start();

        this.mapUpdateListeners = new MapUpdateListeners(this);
        this.mapUpdateListeners.register();
        this.worldEventListener = new WorldEventListener(this);
        this.getServer().getPluginManager().registerEvents(this.worldEventListener, this);

        if (Config.HTTPD_ENABLED) {
            IntegratedServer.startServer();
        } else {
            Logging.info(Lang.LOG_INTERNAL_WEB_DISABLED);
        }
    }

    public void stop() {
        if (this.mapUpdateListeners != null) {
            this.mapUpdateListeners.unregister();
            this.mapUpdateListeners = null;
        }

        if (this.worldEventListener != null) {
            HandlerList.unregisterAll(this.worldEventListener);
            this.worldEventListener = null;
        }

        if (Config.HTTPD_ENABLED) {
            IntegratedServer.stopServer();
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
    public @NonNull PaperWorldManager worldManager() {
        return this.worldManager;
    }

    private void setupApi() {
        this.squaremap = new SquaremapApiProvider(this);
        this.getServer().getServicesManager().register(Squaremap.class, this.squaremap, this, ServicePriority.Normal);
        final Method register = ReflectionUtil.needMethod(SquaremapProvider.class, List.of("register"), Squaremap.class);
        ReflectionUtil.invokeOrThrow(register, null, this.squaremap);
    }

    private void shutdownApi() {
        this.getServer().getServicesManager().unregister(Squaremap.class, this.squaremap);
        final Method unregister = ReflectionUtil.needMethod(SquaremapProvider.class, List.of("unregister"));
        ReflectionUtil.invokeOrThrow(unregister, null);
        this.squaremap = null;
    }

    public @NonNull Squaremap api() {
        return this.squaremap;
    }

    public PaperPlayerManager playerManager() {
        return this.playerManager;
    }

    public SquaremapCommon common() {
        return this.common;
    }

    @Override
    public @NonNull ChunkSnapshotProvider chunkSnapshotProvider() {
        return PaperChunkSnapshotProvider.get();
    }

    @Override
    public @NonNull Path dataDirectory() {
        return this.getDataFolder().toPath();
    }

    @Override
    public @NonNull Logger logger() {
        return this.getLog4JLogger();
    }

    @Override
    public @NonNull ComponentFlattener componentFlattener() {
        return PaperComponents.flattener();
    }

    @Override
    public @NonNull String configNameForWorld(final @NonNull ServerLevel level) {
        return level.getWorld().getName();
    }

    @Override
    public @NonNull String tilesDirNameForWorld(final @NonNull ServerLevel level) {
        return level.getWorld().getName();
    }

    @Override
    public @NonNull Collection<ServerLevel> levels() {
        final List<ServerLevel> levels = new ArrayList<>();
        for (final World world : Bukkit.getWorlds()) {
            levels.add(((CraftWorld) world).getHandle());
        }
        return levels;
    }

    @Override
    public @NonNull Path regionFileDirectory(final @NonNull ServerLevel level) {
        return LevelStorageSource.getStorageFolder(level.getWorld().getWorldFolder().toPath(), level.getTypeKey()).resolve("region");
    }

    @Override
    public @NonNull BiomeSpecialEffectsAccess biomeSpecialEffectsAccess() {
        return CraftBukkitReflection.BiomeSpecialEffectsHelper.get();
    }
}
