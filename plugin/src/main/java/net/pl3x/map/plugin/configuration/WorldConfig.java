package net.pl3x.map.plugin.configuration;

import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"unused"})
public class WorldConfig {
    private static final Map<UUID, WorldConfig> configs = new HashMap<>();

    public static void reload() {
        configs.clear();
        Bukkit.getWorlds().forEach(WorldConfig::load);
    }

    public static WorldConfig get(World world) {
        return configs.get(world.getUID());
    }

    public static void load(final @NonNull World world) {
        configs.put(world.getUID(), new WorldConfig(world));
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
        return Config.CONFIG.getBoolean("world-settings." + worldName + "." + path,
                Config.CONFIG.getBoolean("world-settings.default." + path));
    }

    private int getInt(String path, int def) {
        Config.CONFIG.addDefault("world-settings.default." + path, def);
        return Config.CONFIG.getInt("world-settings." + worldName + "." + path,
                Config.CONFIG.getInt("world-settings.default." + path));
    }

    private String getString(String path, String def) {
        Config.CONFIG.addDefault("world-settings.default." + path, def);
        return Config.CONFIG.getString("world-settings." + worldName + "." + path,
                Config.CONFIG.getString("world-settings.default." + path));
    }

    private <T> List<?> getList(String path, T def) {
        Config.CONFIG.addDefault("world-settings.default." + path, def);
        return Config.CONFIG.getList("world-settings." + worldName + "." + path,
                Config.CONFIG.getList("world-settings.default." + path));
    }

    public boolean MAP_ENABLED = true;
    public String MAP_DISPLAY_NAME = "{world}";
    public int MAX_RENDER_THREADS = 8;
    public boolean MAP_ITERATE_UP = false;
    public int MAP_MAX_HEIGHT = -1;

    private void worldSettings() {
        MAP_ENABLED = getBoolean("map.enabled", MAP_ENABLED);
        MAP_DISPLAY_NAME = getString("map.display-name", MAP_DISPLAY_NAME);
        MAX_RENDER_THREADS = getInt("map.max-render-threads", MAX_RENDER_THREADS);
        MAP_ITERATE_UP = getBoolean("map.iterate-up", MAP_ITERATE_UP);
        MAP_MAX_HEIGHT = getInt("map.max-height", MAP_MAX_HEIGHT);
    }

    public boolean MAP_BIOMES = true;
    public int MAP_BIOMES_BLEND = 4;

    private void biomeSettings() {
        MAP_BIOMES = getBoolean("map.biomes.enabled", MAP_BIOMES);
        MAP_BIOMES_BLEND = MathHelper.clamp(getInt("map.biomes.blend-biomes", MAP_BIOMES_BLEND), 0, 15);
    }

    public boolean MAP_GLASS_CLEAR = true;

    private void glassSettings() {
        MAP_GLASS_CLEAR = getBoolean("map.glass.clear", MAP_GLASS_CLEAR);
    }

    public boolean MAP_LAVA_CHECKERBOARD = true;

    private void lavaSettings() {
        MAP_LAVA_CHECKERBOARD = getBoolean("map.lava.checkerboard", MAP_LAVA_CHECKERBOARD);
    }

    public boolean MAP_WATER_CLEAR = true;
    public boolean MAP_WATER_CHECKERBOARD = false;

    private void waterSettings() {
        MAP_WATER_CLEAR = getBoolean("map.water.clear-depth", MAP_WATER_CLEAR);
        MAP_WATER_CHECKERBOARD = getBoolean("map.water.checkerboard", MAP_WATER_CHECKERBOARD);
    }

    public int ZOOM_MAX = 3;
    public int ZOOM_DEFAULT = 3;
    public int ZOOM_EXTRA = 2;

    private void zoomSettings() {
        ZOOM_MAX = getInt("map.zoom.maximum", ZOOM_MAX);
        ZOOM_DEFAULT = getInt("map.zoom.default", ZOOM_DEFAULT);
        ZOOM_EXTRA = getInt("map.zoom.extra", ZOOM_EXTRA);
    }

    public boolean BACKGROUND_RENDER_ENABLED = true;
    public int BACKGROUND_RENDER_MAX_CHUNKS_PER_INTERVAL = 1024;
    public int BACKGROUND_RENDER_INTERVAL_SECONDS = 15;
    public int BACKGROUND_RENDER_MAX_THREADS = 8;

    private void backgroundRenderSettings() {
        BACKGROUND_RENDER_ENABLED = getBoolean("map.background-render.enabled", BACKGROUND_RENDER_ENABLED);
        BACKGROUND_RENDER_MAX_CHUNKS_PER_INTERVAL = getInt("map.background-render.max-chunks-per-interval", BACKGROUND_RENDER_MAX_CHUNKS_PER_INTERVAL);
        BACKGROUND_RENDER_INTERVAL_SECONDS = getInt("map.background-render.interval-seconds", BACKGROUND_RENDER_INTERVAL_SECONDS);
        BACKGROUND_RENDER_MAX_THREADS = getInt("map.background-render.max-render-threads", BACKGROUND_RENDER_MAX_THREADS);
    }

    public boolean PLAYER_TRACKER_ENABLED = true;
    public boolean PLAYER_TRACKER_SHOW_TOGGLE = true;
    public boolean PLAYER_TRACKER_NAMEPLATE_ENABLED = true;
    public boolean PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD = true;
    public String PLAYER_TRACKER_NAMEPLATE_HEADS_URL = "https://crafatar.com/avatars/{uuid}?size=16&default=MHF_Steve&overlay";
    public boolean PLAYER_TRACKER_HIDE_INVISIBLE = true;
    public boolean PLAYER_TRACKER_HIDE_SPECTATORS = true;

    private void playerTrackerSettings() {
        PLAYER_TRACKER_ENABLED = getBoolean("player-tracker.enabled", PLAYER_TRACKER_ENABLED);
        PLAYER_TRACKER_SHOW_TOGGLE = getBoolean("player-tracker.show-toggle", PLAYER_TRACKER_SHOW_TOGGLE);
        PLAYER_TRACKER_NAMEPLATE_ENABLED = getBoolean("player-tracker.nameplate.enabled", PLAYER_TRACKER_NAMEPLATE_ENABLED);
        PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD = getBoolean("player-tracker.nameplate.show-head", PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD);
        PLAYER_TRACKER_NAMEPLATE_HEADS_URL = getString("player-tracker.nameplate.heads-url", PLAYER_TRACKER_NAMEPLATE_HEADS_URL);
        PLAYER_TRACKER_HIDE_INVISIBLE = getBoolean("player-tracker.hide.invisible", PLAYER_TRACKER_HIDE_INVISIBLE);
        PLAYER_TRACKER_HIDE_SPECTATORS = getBoolean("player-tracker.hide.spectators", PLAYER_TRACKER_HIDE_SPECTATORS);
    }

    public int MARKER_API_UPDATE_INTERVAL_SECONDS = 15;

    public boolean SPAWN_MARKER_ICON_ENABLED = true;
    public boolean SPAWN_MARKER_ICON_SHOW_CONTROLS = true;
    public boolean SPAWN_MARKER_ICON_DEFAULT_HIDDEN = false;
    public int SPAWN_MARKER_ICON_LAYER_PRIORITY = 0;
    public int SPAWN_MARKER_ICON_Z_INDEX = 0;
    public String SPAWN_MARKER_ICON_LABEL = "Spawn";

    private void markerSettings() {
        MARKER_API_UPDATE_INTERVAL_SECONDS = getInt("map.markers.update-interval-seconds", MARKER_API_UPDATE_INTERVAL_SECONDS);

        SPAWN_MARKER_ICON_ENABLED = getBoolean("map.markers.spawn-icon.enabled", SPAWN_MARKER_ICON_ENABLED);
        SPAWN_MARKER_ICON_SHOW_CONTROLS = getBoolean("map.markers.spawn-icon.show-controls", SPAWN_MARKER_ICON_SHOW_CONTROLS);
        SPAWN_MARKER_ICON_DEFAULT_HIDDEN = getBoolean("map.markers.spawn-icon.default-hidden", SPAWN_MARKER_ICON_DEFAULT_HIDDEN);
        SPAWN_MARKER_ICON_LAYER_PRIORITY = getInt("map.markers.spawn-icon.layer-priority", SPAWN_MARKER_ICON_LAYER_PRIORITY);
        SPAWN_MARKER_ICON_Z_INDEX = getInt("map.markers.spawn-icon.z-index", SPAWN_MARKER_ICON_Z_INDEX);
        SPAWN_MARKER_ICON_LABEL = getString("map.markers.spawn-icon.label", SPAWN_MARKER_ICON_LABEL);
    }

    public List<Block> invisibleBlocks = new ArrayList<>();

    private void invisibleBlocks() {
        invisibleBlocks.clear();
        getList("map.invisible-blocks", List.of(
                "minecraft:tall_grass",
                "minecraft:fern",
                "minecraft:grass",
                "minecraft:large_fern"
        )).forEach(block -> invisibleBlocks.add(IRegistry.BLOCK.get(new MinecraftKey(block.toString()))));
    }

    public List<Block> iterateUpBaseBlocks = new ArrayList<>();

    private void iterateUpBaseBlocks() {
        iterateUpBaseBlocks.clear();
        getList("map.iterate-up-base-blocks", List.of(
                "minecraft:netherrack",
                "minecraft:glowstone",
                "minecraft:soul_sand",
                "minecraft:soul_soil",
                "minecraft:gravel",
                "minecraft:warped_nylium",
                "minecraft:crimson_nylium",
                "minecraft:nether_gold_ore",
                "minecraft:ancient_debris",
                "minecraft:nether_quartz_ore",
                "minecraft:magma_block",
                "minecraft:basalt"
        )).forEach(block -> iterateUpBaseBlocks.add(IRegistry.BLOCK.get(new MinecraftKey(block.toString()))));
    }
}
