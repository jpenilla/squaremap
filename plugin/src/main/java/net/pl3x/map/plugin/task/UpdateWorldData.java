package net.pl3x.map.plugin.task;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.configuration.WorldConfig;
import net.pl3x.map.plugin.util.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

public class UpdateWorldData extends BukkitRunnable {
    private final Gson gson = new Gson();

    @Override
    public void run() {
        List<Object> worlds = new ArrayList<>();

        Bukkit.getWorlds().forEach(world -> {
            WorldConfig worldConfig = WorldConfig.get(world);

            if (!worldConfig.MAP_ENABLED) {
                return;
            }

            Map<String, Object> spawn = new HashMap<>();
            Location loc = world.getSpawnLocation();
            spawn.put("x", loc.getBlockX());
            spawn.put("z", loc.getBlockZ());

            Map<String, Object> playerTracker = new HashMap<>();
            playerTracker.put("enabled", worldConfig.PLAYER_TRACKER_ENABLED);
            playerTracker.put("update_interval", worldConfig.PLAYER_TRACKER_UPDATE_INTERVAL);
            playerTracker.put("label", Lang.UI_PLAYER_TRACKER_LABEL);
            playerTracker.put("show_controls", worldConfig.PLAYER_TRACKER_SHOW_CONTROLS);
            playerTracker.put("default_hidden", worldConfig.PLAYER_TRACKER_DEFAULT_HIDDEN);
            playerTracker.put("priority", worldConfig.PLAYER_TRACKER_PRIORITY);
            playerTracker.put("z_index", worldConfig.PLAYER_TRACKER_Z_INDEX);

            Map<String, Object> nameplates = new HashMap<>();
            nameplates.put("enabled", worldConfig.PLAYER_TRACKER_NAMEPLATE_ENABLED);
            nameplates.put("show_heads", worldConfig.PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD);
            nameplates.put("heads_url", worldConfig.PLAYER_TRACKER_NAMEPLATE_HEADS_URL);
            nameplates.put("show_armor", worldConfig.PLAYER_TRACKER_NAMEPLATE_SHOW_ARMOR);
            nameplates.put("show_health", worldConfig.PLAYER_TRACKER_NAMEPLATE_SHOW_HEALTH);
            playerTracker.put("nameplates", nameplates);

            Map<String, Object> zoom = new HashMap<>();
            zoom.put("max", worldConfig.ZOOM_MAX);
            zoom.put("def", worldConfig.ZOOM_DEFAULT);
            zoom.put("extra", worldConfig.ZOOM_EXTRA);

            Map<String, Object> settings = new HashMap<>();
            settings.put("spawn", spawn);
            settings.put("player_tracker", playerTracker);
            settings.put("zoom", zoom);
            settings.put("marker_update_interval", worldConfig.MARKER_API_UPDATE_INTERVAL_SECONDS);
            settings.put("tiles_update_interval", worldConfig.BACKGROUND_RENDER_INTERVAL_SECONDS);

            FileUtil.write(this.gson.toJson(settings), FileUtil.getWorldFolder(world).resolve("settings.json"));

            Map<String, Object> worldsList = new HashMap<>();
            worldsList.put("name", world.getName());
            worldsList.put("display_name", worldConfig.MAP_DISPLAY_NAME
                    .replace("{world}", world.getName()));
            worldsList.put("icon", worldConfig.MAP_ICON);
            worldsList.put("type", world.getEnvironment().name().toLowerCase());
            worldsList.put("order", worldConfig.MAP_ORDER);
            worlds.add(worldsList);
        });

        Map<String, Object> ui = new HashMap<>();
        ui.put("title", Lang.UI_TITLE);

        Map<String, Object> coordinates = new HashMap<>();
        coordinates.put("enabled", Config.UI_COORDINATES_ENABLED);
        coordinates.put("html", Lang.UI_COORDINATES_HTML);
        ui.put("coordinates", coordinates);

        Map<String, Object> link = new HashMap<>();
        link.put("enabled", Config.UI_LINK_ENABLED);
        ui.put("link", link);

        Map<String, Object> sidebar = new HashMap<>();
        sidebar.put("pinned", Config.UI_SIDEBAR_PINNED);
        sidebar.put("player_list_label", Lang.UI_SIDEBAR_PLAYER_LIST_LABEL);
        sidebar.put("world_list_label", Lang.UI_SIDEBAR_WORLD_LIST_LABEL);
        ui.put("sidebar", sidebar);

        Map<String, Object> map = new HashMap<>();
        map.put("worlds", worlds);
        map.put("ui", ui);

        FileUtil.write(this.gson.toJson(map), FileUtil.TILES_DIR.resolve("settings.json"));
    }
}
