package net.pl3x.map.plugin.configuration;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.util.Mth;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

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
        this.init();
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
        this.MAP_ENABLED = getBoolean("map.enabled", this.MAP_ENABLED);
        this.MAP_DISPLAY_NAME = getString("map.display-name", this.MAP_DISPLAY_NAME);
        this.MAP_ORDER = getInt("map.order", this.MAP_ORDER);
        this.MAP_ICON = getString("map.icon", this.MAP_ICON);
        this.MAX_RENDER_THREADS = getInt("map.max-render-threads", this.MAX_RENDER_THREADS);
        this.MAP_ITERATE_UP = getBoolean("map.iterate-up", this.MAP_ITERATE_UP);
        this.MAP_MAX_HEIGHT = getInt("map.max-height", this.MAP_MAX_HEIGHT);
        this.MAP_RENDER_PROGRESS_INTERVAL = getInt("map.render-progress-interval", this.MAP_RENDER_PROGRESS_INTERVAL);
    }

    public boolean MAP_BIOMES = true;
    public int MAP_BIOMES_BLEND = 4;

    private void biomeSettings() {
        this.MAP_BIOMES = getBoolean("map.biomes.enabled", this.MAP_BIOMES);
        this.MAP_BIOMES_BLEND = Mth.clamp(getInt("map.biomes.blend-biomes", this.MAP_BIOMES_BLEND), 0, 15);
    }

    public boolean MAP_GLASS_CLEAR = true;

    private void glassSettings() {
        this.MAP_GLASS_CLEAR = getBoolean("map.glass.clear", this.MAP_GLASS_CLEAR);
    }

    public boolean MAP_LAVA_CHECKERBOARD = true;

    private void lavaSettings() {
        this.MAP_LAVA_CHECKERBOARD = getBoolean("map.lava.checkerboard", this.MAP_LAVA_CHECKERBOARD);
    }

    public boolean MAP_WATER_CLEAR = true;
    public boolean MAP_WATER_CHECKERBOARD = false;

    private void waterSettings() {
        this.MAP_WATER_CLEAR = getBoolean("map.water.clear-depth", this.MAP_WATER_CLEAR);
        this.MAP_WATER_CHECKERBOARD = getBoolean("map.water.checkerboard", this.MAP_WATER_CHECKERBOARD);
    }

    public int ZOOM_MAX = 3;
    public int ZOOM_DEFAULT = 3;
    public int ZOOM_EXTRA = 2;

    private void zoomSettings() {
        this.ZOOM_MAX = getInt("map.zoom.maximum", this.ZOOM_MAX);
        this.ZOOM_DEFAULT = getInt("map.zoom.default", this.ZOOM_DEFAULT);
        this.ZOOM_EXTRA = getInt("map.zoom.extra", this.ZOOM_EXTRA);
    }

    public boolean BACKGROUND_RENDER_ENABLED = true;
    public int BACKGROUND_RENDER_MAX_CHUNKS_PER_INTERVAL = 1024;
    public int BACKGROUND_RENDER_INTERVAL_SECONDS = 15;
    public int BACKGROUND_RENDER_MAX_THREADS = -1;

    private void backgroundRenderSettings() {
        this.BACKGROUND_RENDER_ENABLED = getBoolean("map.background-render.enabled", this.BACKGROUND_RENDER_ENABLED);
        this.BACKGROUND_RENDER_MAX_CHUNKS_PER_INTERVAL = getInt("map.background-render.max-chunks-per-interval", this.BACKGROUND_RENDER_MAX_CHUNKS_PER_INTERVAL);
        this.BACKGROUND_RENDER_INTERVAL_SECONDS = getInt("map.background-render.interval-seconds", this.BACKGROUND_RENDER_INTERVAL_SECONDS);
        this.BACKGROUND_RENDER_MAX_THREADS = getInt("map.background-render.max-render-threads", this.BACKGROUND_RENDER_MAX_THREADS);
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
        this.PLAYER_TRACKER_ENABLED = getBoolean("player-tracker.enabled", this.PLAYER_TRACKER_ENABLED);
        this.PLAYER_TRACKER_UPDATE_INTERVAL = getInt("player-tracker.update-interval-seconds", this.PLAYER_TRACKER_UPDATE_INTERVAL);
        this.PLAYER_TRACKER_SHOW_CONTROLS = getBoolean("player-tracker.show-controls", this.PLAYER_TRACKER_SHOW_CONTROLS);
        this.PLAYER_TRACKER_DEFAULT_HIDDEN = getBoolean("player-tracker.default-hidden", this.PLAYER_TRACKER_DEFAULT_HIDDEN);
        this.PLAYER_TRACKER_PRIORITY = getInt("player-tracker.layer-priority", this.PLAYER_TRACKER_PRIORITY);
        this.PLAYER_TRACKER_Z_INDEX = getInt("player-tracker.z-index", this.PLAYER_TRACKER_Z_INDEX);
        this.PLAYER_TRACKER_NAMEPLATE_ENABLED = getBoolean("player-tracker.nameplate.enabled", this.PLAYER_TRACKER_NAMEPLATE_ENABLED);
        this.PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD = getBoolean("player-tracker.nameplate.show-head", this.PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD);
        this.PLAYER_TRACKER_NAMEPLATE_HEADS_URL = getString("player-tracker.nameplate.heads-url", this.PLAYER_TRACKER_NAMEPLATE_HEADS_URL);
        this.PLAYER_TRACKER_NAMEPLATE_SHOW_ARMOR = getBoolean("player-tracker.nameplate.show-armor", this.PLAYER_TRACKER_NAMEPLATE_SHOW_ARMOR);
        this.PLAYER_TRACKER_NAMEPLATE_SHOW_HEALTH = getBoolean("player-tracker.nameplate.show-health", this.PLAYER_TRACKER_NAMEPLATE_SHOW_HEALTH);
        this.PLAYER_TRACKER_HIDE_INVISIBLE = getBoolean("player-tracker.hide.invisible", this.PLAYER_TRACKER_HIDE_INVISIBLE);
        this.PLAYER_TRACKER_HIDE_SPECTATORS = getBoolean("player-tracker.hide.spectators", this.PLAYER_TRACKER_HIDE_SPECTATORS);
    }

    public int MARKER_API_UPDATE_INTERVAL_SECONDS = 5;

    private void markerSettings() {
        this.MARKER_API_UPDATE_INTERVAL_SECONDS = getInt("map.markers.update-interval-seconds", this.MARKER_API_UPDATE_INTERVAL_SECONDS);
    }

    public boolean SPAWN_MARKER_ICON_ENABLED = true;
    public boolean SPAWN_MARKER_ICON_SHOW_CONTROLS = true;
    public boolean SPAWN_MARKER_ICON_DEFAULT_HIDDEN = false;
    public int SPAWN_MARKER_ICON_LAYER_PRIORITY = 0;
    public int SPAWN_MARKER_ICON_Z_INDEX = 0;

    private void spawnMarkerSettings() {
        this.SPAWN_MARKER_ICON_ENABLED = getBoolean("map.markers.spawn-icon.enabled", this.SPAWN_MARKER_ICON_ENABLED);
        this.SPAWN_MARKER_ICON_SHOW_CONTROLS = getBoolean("map.markers.spawn-icon.show-controls", this.SPAWN_MARKER_ICON_SHOW_CONTROLS);
        this.SPAWN_MARKER_ICON_DEFAULT_HIDDEN = getBoolean("map.markers.spawn-icon.default-hidden", this.SPAWN_MARKER_ICON_DEFAULT_HIDDEN);
        this.SPAWN_MARKER_ICON_LAYER_PRIORITY = getInt("map.markers.spawn-icon.layer-priority", this.SPAWN_MARKER_ICON_LAYER_PRIORITY);
        this.SPAWN_MARKER_ICON_Z_INDEX = getInt("map.markers.spawn-icon.z-index", this.SPAWN_MARKER_ICON_Z_INDEX);
    }

    public boolean WORLDBORDER_MARKER_ENABLED = true;
    public boolean WORLDBORDER_MARKER_SHOW_CONTROLS = true;
    public boolean WORLDBORDER_MARKER_DEFAULT_HIDDEN = false;
    public int WORLDBORDER_MARKER_LAYER_PRIORITY = 1;
    public int WORLDBORDER_MARKER_Z_INDEX = 1;

    private void worldborderMarkerSettings() {
        this.WORLDBORDER_MARKER_ENABLED = getBoolean("map.markers.world-border.enabled", this.WORLDBORDER_MARKER_ENABLED);
        this.WORLDBORDER_MARKER_SHOW_CONTROLS = getBoolean("map.markers.world-border.show-controls", this.WORLDBORDER_MARKER_SHOW_CONTROLS);
        this.WORLDBORDER_MARKER_DEFAULT_HIDDEN = getBoolean("map.markers.world-border.default-hidden", this.WORLDBORDER_MARKER_DEFAULT_HIDDEN);
        this.WORLDBORDER_MARKER_LAYER_PRIORITY = getInt("map.markers.world-border.layer-priority", this.WORLDBORDER_MARKER_LAYER_PRIORITY);
        this.WORLDBORDER_MARKER_Z_INDEX = getInt("map.markers.world-border.z-index", this.WORLDBORDER_MARKER_Z_INDEX);
    }

    public List<Map<String, Object>> VISIBILITY_LIMITS = new ArrayList<>();

    @SuppressWarnings("unchecked") // Safe, as YAML can only store dicts of <String, object>
    private void visibilityLimitSettings() {
        this.VISIBILITY_LIMITS = (List<Map<String, Object>>) this.getList("map.visibility-limits", Collections.singletonList(
                ImmutableMap.of("type", "world-border", "enabled", false)));
    }

}
