package net.pl3x.map.plugin.configuration;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.Mth;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public class WorldConfig extends AbstractWorldConfig {
    private static final Map<UUID, WorldConfig> configs = new HashMap<>();

    public static void reload() {
        configs.clear();
        Bukkit.getWorlds().forEach(WorldConfig::get);
    }

    public static WorldConfig get(final @NonNull World world) {
        WorldConfig config = configs.get(world.getUID());
        if (config == null) {
            config = new WorldConfig(world, Config.config);
            configs.put(world.getUID(), config);
        }
        return config;
    }

    WorldConfig(World world, AbstractConfig parent) {
        super(world, parent);
        init();
    }

    void init() {
        this.config.readConfig(WorldConfig.class, this);
    }

    public boolean MAP_ENABLED = true;
    public String MAP_DISPLAY_NAME = "{world}";
    public int MAP_ORDER = 0;
    public String MAP_ICON = "";
    public int MAX_RENDER_THREADS = -1;
    public boolean MAP_ITERATE_UP = false;
    public int MAP_MAX_HEIGHT = -1;
    public int MAP_RENDER_PROGRESS_INTERVAL = 1;

    private void worldSettings() {
        MAP_ENABLED = getBoolean("map.enabled", MAP_ENABLED);
        MAP_DISPLAY_NAME = getString("map.display-name", MAP_DISPLAY_NAME);
        MAP_ORDER = getInt("map.order", MAP_ORDER);
        MAP_ICON = getString("map.icon", MAP_ICON);
        MAX_RENDER_THREADS = getInt("map.max-render-threads", MAX_RENDER_THREADS);
        MAP_ITERATE_UP = getBoolean("map.iterate-up", MAP_ITERATE_UP);
        MAP_MAX_HEIGHT = getInt("map.max-height", MAP_MAX_HEIGHT);
        MAP_RENDER_PROGRESS_INTERVAL = getInt("map.render-progress-interval", MAP_RENDER_PROGRESS_INTERVAL);
    }

    public boolean MAP_BIOMES = true;
    public int MAP_BIOMES_BLEND = 4;

    private void biomeSettings() {
        MAP_BIOMES = getBoolean("map.biomes.enabled", MAP_BIOMES);
        MAP_BIOMES_BLEND = Mth.clamp(getInt("map.biomes.blend-biomes", MAP_BIOMES_BLEND), 0, 15);
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
    public int BACKGROUND_RENDER_MAX_THREADS = -1;

    private void backgroundRenderSettings() {
        BACKGROUND_RENDER_ENABLED = getBoolean("map.background-render.enabled", BACKGROUND_RENDER_ENABLED);
        BACKGROUND_RENDER_MAX_CHUNKS_PER_INTERVAL = getInt("map.background-render.max-chunks-per-interval", BACKGROUND_RENDER_MAX_CHUNKS_PER_INTERVAL);
        BACKGROUND_RENDER_INTERVAL_SECONDS = getInt("map.background-render.interval-seconds", BACKGROUND_RENDER_INTERVAL_SECONDS);
        BACKGROUND_RENDER_MAX_THREADS = getInt("map.background-render.max-render-threads", BACKGROUND_RENDER_MAX_THREADS);
    }

    public boolean PLAYER_TRACKER_ENABLED = true;
    public int PLAYER_TRACKER_UPDATE_INTERVAL = 1;
    public boolean PLAYER_TRACKER_SHOW_CONTROLS = true;
    public boolean PLAYER_TRACKER_DEFAULT_HIDDEN = false;
    public int PLAYER_TRACKER_PRIORITY = 2;
    public int PLAYER_TRACKER_Z_INDEX = 2;
    public boolean PLAYER_TRACKER_NAMEPLATE_ENABLED = true;
    public boolean PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD = true;
    public String PLAYER_TRACKER_NAMEPLATE_HEADS_URL = "https://mc-heads.net/avatar/{uuid}/16";
    public boolean PLAYER_TRACKER_NAMEPLATE_SHOW_ARMOR = true;
    public boolean PLAYER_TRACKER_NAMEPLATE_SHOW_HEALTH = true;
    public boolean PLAYER_TRACKER_HIDE_INVISIBLE = true;
    public boolean PLAYER_TRACKER_HIDE_SPECTATORS = true;

    private void playerTrackerSettings() {
        PLAYER_TRACKER_ENABLED = getBoolean("player-tracker.enabled", PLAYER_TRACKER_ENABLED);
        PLAYER_TRACKER_UPDATE_INTERVAL = getInt("player-tracker.update-interval-seconds", PLAYER_TRACKER_UPDATE_INTERVAL);
        PLAYER_TRACKER_SHOW_CONTROLS = getBoolean("player-tracker.show-controls", PLAYER_TRACKER_SHOW_CONTROLS);
        PLAYER_TRACKER_DEFAULT_HIDDEN = getBoolean("player-tracker.default-hidden", PLAYER_TRACKER_DEFAULT_HIDDEN);
        PLAYER_TRACKER_PRIORITY = getInt("player-tracker.layer-priority", PLAYER_TRACKER_PRIORITY);
        PLAYER_TRACKER_Z_INDEX = getInt("player-tracker.z-index", PLAYER_TRACKER_Z_INDEX);
        PLAYER_TRACKER_NAMEPLATE_ENABLED = getBoolean("player-tracker.nameplate.enabled", PLAYER_TRACKER_NAMEPLATE_ENABLED);
        PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD = getBoolean("player-tracker.nameplate.show-head", PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD);
        PLAYER_TRACKER_NAMEPLATE_HEADS_URL = getString("player-tracker.nameplate.heads-url", PLAYER_TRACKER_NAMEPLATE_HEADS_URL);
        PLAYER_TRACKER_NAMEPLATE_SHOW_ARMOR = getBoolean("player-tracker.nameplate.show-armor", PLAYER_TRACKER_NAMEPLATE_SHOW_ARMOR);
        PLAYER_TRACKER_NAMEPLATE_SHOW_HEALTH = getBoolean("player-tracker.nameplate.show-health", PLAYER_TRACKER_NAMEPLATE_SHOW_HEALTH);
        PLAYER_TRACKER_HIDE_INVISIBLE = getBoolean("player-tracker.hide.invisible", PLAYER_TRACKER_HIDE_INVISIBLE);
        PLAYER_TRACKER_HIDE_SPECTATORS = getBoolean("player-tracker.hide.spectators", PLAYER_TRACKER_HIDE_SPECTATORS);
    }

    public int MARKER_API_UPDATE_INTERVAL_SECONDS = 5;

    private void markerSettings() {
        MARKER_API_UPDATE_INTERVAL_SECONDS = getInt("map.markers.update-interval-seconds", MARKER_API_UPDATE_INTERVAL_SECONDS);
    }

    public boolean SPAWN_MARKER_ICON_ENABLED = true;
    public boolean SPAWN_MARKER_ICON_SHOW_CONTROLS = true;
    public boolean SPAWN_MARKER_ICON_DEFAULT_HIDDEN = false;
    public int SPAWN_MARKER_ICON_LAYER_PRIORITY = 0;
    public int SPAWN_MARKER_ICON_Z_INDEX = 0;

    private void spawnMarkerSettings() {
        SPAWN_MARKER_ICON_ENABLED = getBoolean("map.markers.spawn-icon.enabled", SPAWN_MARKER_ICON_ENABLED);
        SPAWN_MARKER_ICON_SHOW_CONTROLS = getBoolean("map.markers.spawn-icon.show-controls", SPAWN_MARKER_ICON_SHOW_CONTROLS);
        SPAWN_MARKER_ICON_DEFAULT_HIDDEN = getBoolean("map.markers.spawn-icon.default-hidden", SPAWN_MARKER_ICON_DEFAULT_HIDDEN);
        SPAWN_MARKER_ICON_LAYER_PRIORITY = getInt("map.markers.spawn-icon.layer-priority", SPAWN_MARKER_ICON_LAYER_PRIORITY);
        SPAWN_MARKER_ICON_Z_INDEX = getInt("map.markers.spawn-icon.z-index", SPAWN_MARKER_ICON_Z_INDEX);
    }

    public boolean WORLDBORDER_MARKER_ENABLED = true;
    public boolean WORLDBORDER_MARKER_SHOW_CONTROLS = true;
    public boolean WORLDBORDER_MARKER_DEFAULT_HIDDEN = false;
    public int WORLDBORDER_MARKER_LAYER_PRIORITY = 1;
    public int WORLDBORDER_MARKER_Z_INDEX = 1;

    private void worldborderMarkerSettings() {
        WORLDBORDER_MARKER_ENABLED = getBoolean("map.markers.world-border.enabled", WORLDBORDER_MARKER_ENABLED);
        WORLDBORDER_MARKER_SHOW_CONTROLS = getBoolean("map.markers.world-border.show-controls", WORLDBORDER_MARKER_SHOW_CONTROLS);
        WORLDBORDER_MARKER_DEFAULT_HIDDEN = getBoolean("map.markers.world-border.default-hidden", WORLDBORDER_MARKER_DEFAULT_HIDDEN);
        WORLDBORDER_MARKER_LAYER_PRIORITY = getInt("map.markers.world-border.layer-priority", WORLDBORDER_MARKER_LAYER_PRIORITY);
        WORLDBORDER_MARKER_Z_INDEX = getInt("map.markers.world-border.z-index", WORLDBORDER_MARKER_Z_INDEX);
    }

    public List<Map<String, Object>> VISIBILITY_LIMITS = new ArrayList<>();

    @SuppressWarnings("unchecked") // Safe, as YAML can only store dicts of <String, object>
    private void visibilityLimitSettings() {
        VISIBILITY_LIMITS = (List<Map<String, Object>>) this.getList("map.visibility-limits", Collections.singletonList(
                ImmutableMap.of("type", "world-border", "enabled", false)));
    }

}
