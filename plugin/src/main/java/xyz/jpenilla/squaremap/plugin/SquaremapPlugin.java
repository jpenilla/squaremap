package xyz.jpenilla.squaremap.plugin;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import javax.imageio.ImageIO;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.plugin.api.PlayerManager;
import xyz.jpenilla.squaremap.plugin.api.SpawnIconProvider;
import xyz.jpenilla.squaremap.plugin.api.SquaremapApiProvider;
import xyz.jpenilla.squaremap.plugin.command.Commands;
import xyz.jpenilla.squaremap.plugin.config.Advanced;
import xyz.jpenilla.squaremap.plugin.config.Config;
import xyz.jpenilla.squaremap.plugin.config.Lang;
import xyz.jpenilla.squaremap.plugin.httpd.IntegratedServer;
import xyz.jpenilla.squaremap.plugin.listener.MapUpdateListeners;
import xyz.jpenilla.squaremap.plugin.listener.PlayerListener;
import xyz.jpenilla.squaremap.plugin.listener.WorldEventListener;
import xyz.jpenilla.squaremap.plugin.network.Network;
import xyz.jpenilla.squaremap.plugin.task.UpdatePlayers;
import xyz.jpenilla.squaremap.plugin.task.UpdateWorldData;
import xyz.jpenilla.squaremap.plugin.util.FileUtil;
import xyz.jpenilla.squaremap.plugin.util.ReflectionUtil;

public final class SquaremapPlugin extends JavaPlugin {
    private static SquaremapPlugin instance;
    private Squaremap squaremap;
    private WorldManager worldManager;
    private PlayerManager playerManager;
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
        this.playerManager = new PlayerManager();

        this.updatePlayers = new UpdatePlayers(this);
        this.updatePlayers.runTaskTimer(this, 20, 20);

        this.updateWorldData = new UpdateWorldData();
        this.updateWorldData.runTaskTimer(this, 0, 20 * 5);

        this.worldManager = new WorldManager();
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

    public WorldManager worldManager() {
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

    public PlayerManager playerManager() {
        return this.playerManager;
    }
}
