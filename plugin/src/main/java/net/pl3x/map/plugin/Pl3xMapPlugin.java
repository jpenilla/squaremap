package net.pl3x.map.plugin;

import net.pl3x.map.api.Key;
import net.pl3x.map.api.LayerProvider;
import net.pl3x.map.api.Pl3xMap;
import net.pl3x.map.api.Pl3xMapProvider;
import net.pl3x.map.api.Point;
import net.pl3x.map.api.marker.Marker;
import net.pl3x.map.api.marker.MarkerOptions;
import net.pl3x.map.plugin.api.Pl3xMapApiProvider;
import net.pl3x.map.plugin.command.CommandManager;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.httpd.IntegratedServer;
import net.pl3x.map.plugin.task.UpdateMarkers;
import net.pl3x.map.plugin.task.UpdatePlayers;
import net.pl3x.map.plugin.task.UpdateWorldData;
import net.pl3x.map.plugin.util.FileUtil;
import net.pl3x.map.plugin.util.ReflectionUtil;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

public final class Pl3xMapPlugin extends JavaPlugin {
    private static Pl3xMapPlugin instance;
    private Pl3xMap pl3xMap;
    private WorldManager worldManager;
    private UpdateWorldData updateWorldData;
    private UpdatePlayers updatePlayers;
    private UpdateMarkers updateMarkers;
    private MapUpdateListeners mapUpdateListeners;

    public Pl3xMapPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
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

        final Key spawnIconKey = Key.of("pl3xmap-spawn_icon");
        try {
            this.getApi().iconRegistry().register(spawnIconKey, ImageIO.read(FileUtil.WEB_DIR.resolve("images/icon/spawn.png").toFile()));
        } catch (IOException e) {
            Logger.log().log(Level.WARNING, "Failed to register spawn icon", e);
        }
        this.worldManager.worlds().values().forEach(world -> world.layerRegistry().register(spawnIconKey, new LayerProvider() {

            @Override
            public @NonNull String getLabel() {
                return "Spawn";
            }

            @Override
            public boolean showControls() {
                return true;
            }

            @Override
            public boolean defaultHidden() {
                return false;
            }

            @Override
            public int layerPriority() {
                return 0;
            }

            @Override
            public int zIndex() {
                return 0;
            }

            private final MarkerOptions options = MarkerOptions.builder().hoverTooltip("Spawn").build();

            @Override
            public @NonNull Collection<Marker> getMarkers() {
                return Collections.singletonList(Marker.icon(
                        Point.fromLocation(world.bukkit().getSpawnLocation()),
                        spawnIconKey,
                        16
                ).markerOptions(this.options));
            }

        }));
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

        this.updatePlayers = new UpdatePlayers();
        this.updatePlayers.runTaskTimer(this, 20, 20);
        this.updateWorldData = new UpdateWorldData();
        this.updateWorldData.runTaskTimer(this, 0, 20 * 5);

        this.worldManager = new WorldManager();
        this.worldManager.start();

        this.updateMarkers = new UpdateMarkers(this);
        this.updateMarkers.runTaskTimer(this, 20 * 5, 20 * 5); // todo: config

        this.mapUpdateListeners = new MapUpdateListeners(this);
        this.mapUpdateListeners.register();

        if (Config.HTTPD_ENABLED) {
            IntegratedServer.startServer();
        }
    }

    public void stop() {
        this.mapUpdateListeners.unregister();
        this.mapUpdateListeners = null;

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

}
