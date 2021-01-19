package net.pl3x.map.plugin;

import net.pl3x.map.api.Key;
import net.pl3x.map.api.LayerProvider;
import net.pl3x.map.api.Pl3xMap;
import net.pl3x.map.api.Pl3xMapProvider;
import net.pl3x.map.api.Point;
import net.pl3x.map.api.SimpleLayerProvider;
import net.pl3x.map.api.marker.Circle;
import net.pl3x.map.api.marker.Marker;
import net.pl3x.map.api.marker.MarkerOptions;
import net.pl3x.map.api.marker.MultiPolygon;
import net.pl3x.map.api.marker.Polygon;
import net.pl3x.map.api.marker.Polyline;
import net.pl3x.map.api.marker.Rectangle;
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
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.imageio.ImageIO;
import java.awt.Color;
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

        try {
            this.getApi().iconRegistry().register(Key.of("pl3xmap-spawn_icon"), ImageIO.read(FileUtil.WEB_DIR.resolve("images/icon/spawn.png").toFile()));
        } catch (IOException e) {
            Logger.info("Failed to register spawn icon");
        }
        this.worldManager.worlds().values().forEach(world -> world.layerRegistry().register(Key.key("pl3xmap-spawn_icon"), new LayerProvider() {

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
            public @NonNull Collection<Marker> getMarkers() {
                return Collections.singletonList(Marker.icon(
                        Point.fromLocation(world.bukkit().getSpawnLocation()),
                        Key.of("pl3xmap-spawn_icon"),
                        16,
                        16
                ));
            }

        }));

        final SimpleLayerProvider provider = SimpleLayerProvider.builder("My amazing label").build();
        this.worldManager.getWorld(Bukkit.getWorlds().get(0)).layerRegistry().register(Key.of("myLayer"), provider);

        final Rectangle rect = Marker.rectangle(Point.of(0, 0), Point.of(100, 100));
        rect.markerOptions(MarkerOptions.builder().fillColor(Color.GREEN).fillOpacity(0.5));

        final Polyline polyline = Marker.polyline(Point.of(100, 100), Point.of(200, 200));
        polyline.markerOptions(MarkerOptions.builder().strokeColor(Color.YELLOW));

        final Circle circle = Marker.circle(Point.of(-100, -100), 50);
        circle.markerOptions(MarkerOptions.builder().strokeColor(Color.PINK).tooltip("Hello world!"));

        final Polygon polygon = Marker.polygon(Point.of(333, 555), Point.of(123, 456), Point.of(-100, 50));

        final MultiPolygon multiPolygon = Marker.multiPolygon(
                MultiPolygon.subPolygon(
                        new Point[]{Point.of(0, 0), Point.of(0, 100), Point.of(100, 100), Point.of(100, 0)},
                        new Point[]{Point.of(0, 0), Point.of(0, 25), Point.of(25, 25), Point.of(25, 0)}
                ),
                MultiPolygon.subPolygon(Point.of(0, 0), Point.of(-100, -300), Point.of(-50, -500))
        );

        //provider.addMarker(Key.of("mulitpolygon"), multiPolygon);
        provider.addMarker(Key.of("rect"), rect);
        provider.addMarker(Key.of("polyline"), polyline);
        provider.addMarker(Key.of("circle"), circle);
        provider.addMarker(Key.of("polygon"), polygon);
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
