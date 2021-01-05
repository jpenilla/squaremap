package net.pl3x.map.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;

@SuppressWarnings("unused")
public class WorldConfig {
    private static final Map<UUID, WorldConfig> configs = new HashMap<>();

    public static void reload() {
        configs.clear();
        Bukkit.getWorlds().forEach(world -> {
            final WorldConfig config = new WorldConfig(world);
            configs.put(world.getUID(), config);
        });
    }

    public static WorldConfig get(World world) {
        return configs.get(world.getUID());
    }

    private final String worldName;

    public WorldConfig(World world) {
        this.worldName = world.getName();
        init();
    }

    public void init() {
        Config.readConfig(WorldConfig.class, this);
    }

    private void set(String path, Object val) {
        Config.CONFIG.addDefault("world-settings.default." + path, val);
        Config.CONFIG.set("world-settings.default." + path, val);
        if (Config.CONFIG.get("world-settings." + worldName + "." + path) != null) {
            Config.CONFIG.addDefault("world-settings." + worldName + "." + path, val);
            Config.CONFIG.set("world-settings." + worldName + "." + path, val);
        }
    }

    private boolean getBoolean(String path, boolean def) {
        Config.CONFIG.addDefault("world-settings.default." + path, def);
        return Config.CONFIG.getBoolean("world-settings." + worldName + "." + path, Config.CONFIG.getBoolean("world-settings.default." + path));
    }

    private int getInt(String path, int def) {
        Config.CONFIG.addDefault("world-settings.default." + path, def);
        return Config.CONFIG.getInt("world-settings." + worldName + "." + path, Config.CONFIG.getInt("world-settings.default." + path));
    }

    private String getString(String path, String def) {
        Config.CONFIG.addDefault("world-settings.default." + path, def);
        return Config.CONFIG.getString("world-settings." + worldName + "." + path, Config.CONFIG.getString("world-settings.default." + path));
    }

    public boolean MAP_ENABLED = true;
    public boolean MAP_BIOMES = true;
    public boolean MAP_WATER_BIOMES = true;

    private void worldSettings() {
        MAP_ENABLED = getBoolean("map.enabled", MAP_ENABLED);
        MAP_BIOMES = getBoolean("map.biomes", MAP_BIOMES);
        MAP_WATER_BIOMES = getBoolean("map.water-biomes", MAP_WATER_BIOMES);
    }

    public boolean PLAYER_TRACKER_ENABLED = true;
    public boolean PLAYER_TRACKER_SHOW_TOGGLE = true;
    public boolean PLAYER_TRACKER_NAMEPLATE_ENABLED = true;
    public boolean PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD = true;
    public boolean PLAYER_TRACKER_HIDE_INVISIBLE = true;
    public boolean PLAYER_TRACKER_HIDE_SPECTATORS = true;

    private void playerTrackerSettings() {
        PLAYER_TRACKER_ENABLED = getBoolean("player-tracker.enabled", PLAYER_TRACKER_ENABLED);
        PLAYER_TRACKER_SHOW_TOGGLE = getBoolean("player-tracker.show-toggle", PLAYER_TRACKER_SHOW_TOGGLE);
        PLAYER_TRACKER_NAMEPLATE_ENABLED = getBoolean("player-tracker.nameplate.enabled", PLAYER_TRACKER_NAMEPLATE_ENABLED);
        PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD = getBoolean("player-tracker.nameplate.show-head", PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD);
        PLAYER_TRACKER_HIDE_INVISIBLE = getBoolean("player-tracker.hide.invisible", PLAYER_TRACKER_HIDE_INVISIBLE);
        PLAYER_TRACKER_HIDE_SPECTATORS = getBoolean("player-tracker.hide.spectators", PLAYER_TRACKER_HIDE_SPECTATORS);
    }

    public String UI_TITLE = "Pl3xMap";
    public boolean UI_COORDINATES = true;

    private void uiSettings() {
        UI_TITLE = getString("ui.title", UI_TITLE);
        UI_COORDINATES = getBoolean("ui.coordinates", UI_COORDINATES);
    }
}
