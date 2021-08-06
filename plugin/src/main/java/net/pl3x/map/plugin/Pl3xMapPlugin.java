package net.pl3x.map.plugin;

import net.pl3x.map.api.Pl3xMap;
import net.pl3x.map.api.Pl3xMapProvider;
import net.pl3x.map.plugin.api.Pl3xMapApiProvider;
import net.pl3x.map.plugin.api.PlayerManager;
import net.pl3x.map.plugin.api.SpawnIconProvider;
import net.pl3x.map.plugin.command.CommandManager;
import net.pl3x.map.plugin.configuration.Advanced;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.httpd.IntegratedServer;
import net.pl3x.map.plugin.listener.MapUpdateListeners;
import net.pl3x.map.plugin.listener.PlayerListener;
import net.pl3x.map.plugin.listener.WorldEventListener;
import net.pl3x.map.plugin.network.Network;
import net.pl3x.map.plugin.task.UpdatePlayers;
import net.pl3x.map.plugin.task.UpdateWorldData;
import net.pl3x.map.plugin.util.FileUtil;
import net.pl3x.map.plugin.util.ReflectionUtil;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

public final class Pl3xMapPlugin extends JavaPlugin {
    private static Pl3xMapPlugin instance;
    private Pl3xMap pl3xMap;
    private WorldManager worldManager;
    private PlayerManager playerManager;
    private UpdateWorldData updateWorldData;
    private UpdatePlayers updatePlayers;
    private MapUpdateListeners mapUpdateListeners;
    private WorldEventListener worldEventListener;

    public Pl3xMapPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
        } catch (ClassNotFoundException e) {
            Logger.severe("This plugin requires Paper or one of its forks to run");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Config.reload();

        // this has to load after config.yml in order to know if web dir should be overwritten
        // but also before advanced.yml to ensure foliage.png and grass.png are already on disk
        FileUtil.extract("/web/", FileUtil.WEB_DIR.toFile(), Config.UPDATE_WEB_DIR);
        FileUtil.extract("/locale/", FileUtil.LOCALE_DIR.toFile(), false);

        Advanced.reload();
        Lang.reload();

        try {
            new CommandManager(this);
        } catch (Exception e) {
            this.getLogger().log(Level.WARNING, "Failed to initialize command manager", e);
            this.setEnabled(false);
            return;
        }

        this.start();
        this.setupApi();

        try {
            this.api().iconRegistry().register(SpawnIconProvider.SPAWN_ICON_KEY, ImageIO.read(FileUtil.WEB_DIR.resolve("images/icon/spawn.png").toFile()));
        } catch (IOException e) {
            Logger.log().log(Level.WARNING, "Failed to register spawn icon", e);
        }

        Network.register();

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        new Metrics(this, 10133); // https://bstats.org/plugin/bukkit/Pl3xMap/10133
    }

    @Override
    public void onDisable() {
        Network.unregister();
        this.shutdownApi();
        this.stop();
    }

    public static Pl3xMapPlugin getInstance() {
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
            Logger.info(Lang.LOG_INTERNAL_WEB_DISABLED);
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

        if (playerManager != null) {
            this.playerManager = null;
        }

        this.getServer().getScheduler().cancelTasks(this);
    }

    public @NonNull WorldManager worldManager() {
        return this.worldManager;
    }

    private void setupApi() {
        this.pl3xMap = new Pl3xMapApiProvider(this);
        this.getServer().getServicesManager().register(Pl3xMap.class, this.pl3xMap, this, ServicePriority.Normal);
        final Method register = ReflectionUtil.needMethod(Pl3xMapProvider.class, List.of("register"), Pl3xMap.class);
        ReflectionUtil.invokeOrThrow(register, null, this.pl3xMap);
    }

    private void shutdownApi() {
        this.getServer().getServicesManager().unregister(Pl3xMap.class, this.pl3xMap);
        final Method unregister = ReflectionUtil.needMethod(Pl3xMapProvider.class, List.of("unregister"));
        ReflectionUtil.invokeOrThrow(unregister, null);
        this.pl3xMap = null;
    }

    public @NonNull Pl3xMap api() {
        return this.pl3xMap;
    }

    public PlayerManager playerManager() {
        return this.playerManager;
    }
}
