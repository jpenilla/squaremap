package net.pl3x.map.plugin;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.pl3x.map.api.Pl3xMap;
import net.pl3x.map.api.Pl3xMapProvider;
import net.pl3x.map.plugin.api.Pl3xMapApiProvider;
import net.pl3x.map.plugin.api.SpawnIconProvider;
import net.pl3x.map.plugin.command.CommandManager;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.httpd.IntegratedServer;
import net.pl3x.map.plugin.listener.MapUpdateListeners;
import net.pl3x.map.plugin.listener.WorldEventListener;
import net.pl3x.map.plugin.task.UpdatePlayers;
import net.pl3x.map.plugin.task.UpdateWorldData;
import net.pl3x.map.plugin.util.FileUtil;
import net.pl3x.map.plugin.util.ReflectionUtil;
import net.pl3x.map.plugin.util.PlayerManager;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.lang.reflect.Method;
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
    private BukkitAudiences audiences;

    public Pl3xMapPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.audiences = BukkitAudiences.create(this);
        Config.reload();
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
            this.getApi().iconRegistry().register(SpawnIconProvider.SPAWN_ICON_KEY, ImageIO.read(FileUtil.WEB_DIR.resolve("images/icon/spawn.png").toFile()));
        } catch (IOException e) {
            Logger.log().log(Level.WARNING, "Failed to register spawn icon", e);
        }
    }

    @Override
    public void onDisable() {
        this.shutdownApi();
        this.stop();
    }

    public static Pl3xMapPlugin getInstance() {
        return instance;
    }

    public void start() {
        FileUtil.extractWebFolder();

        this.updatePlayers = new UpdatePlayers(this);
        this.updatePlayers.runTaskTimer(this, 20, 20);
        this.updateWorldData = new UpdateWorldData();
        this.updateWorldData.runTaskTimer(this, 0, 20 * 5);

        this.worldManager = new WorldManager();
        this.worldManager.start();

        this.playerManager = new PlayerManager();

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
        this.mapUpdateListeners.unregister();
        this.mapUpdateListeners = null;
        HandlerList.unregisterAll(this.worldEventListener);
        this.worldEventListener = null;

        if (Config.HTTPD_ENABLED) {
            IntegratedServer.stopServer();
        }

        if (this.updatePlayers != null && !this.updatePlayers.isCancelled()) {
            this.updatePlayers.cancel();
            this.updatePlayers = null;
        }
        if (this.updateWorldData != null && !this.updateWorldData.isCancelled()) {
            this.updateWorldData.cancel();
            this.updateWorldData = null;
        }

        this.worldManager.shutdown();
        this.worldManager = null;

        this.getServer().getScheduler().cancelTasks(this);
    }

    public @NonNull WorldManager worldManager() {
        return this.worldManager;
    }

    public @NonNull PlayerManager playerManager() {
        return this.playerManager;
    }

    private void setupApi() {
        this.pl3xMap = new Pl3xMapApiProvider(this);
        this.getServer().getServicesManager().register(Pl3xMap.class, this.pl3xMap, this, ServicePriority.Normal);
        final Method register = ReflectionUtil.needMethod(Pl3xMapProvider.class, "register", Pl3xMap.class);
        ReflectionUtil.invokeOrThrow(register, null, this.pl3xMap);
    }

    private void shutdownApi() {
        this.getServer().getServicesManager().unregister(Pl3xMap.class, this.pl3xMap);
        final Method unregister = ReflectionUtil.needMethod(Pl3xMapProvider.class, "unregister");
        ReflectionUtil.invokeOrThrow(unregister, null);
        this.pl3xMap = null;
    }

    public @NonNull Pl3xMap getApi() {
        return this.pl3xMap;
    }

    public @NonNull BukkitAudiences audiences() {
        return this.audiences;
    }

}
