package net.pl3x.map.plugin.task;

import com.google.gson.Gson;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.WorldConfig;
import net.pl3x.map.plugin.util.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            playerTracker.put("show_toggle", worldConfig.PLAYER_TRACKER_SHOW_TOGGLE);

            Map<String, Object> nameplates = new HashMap<>();
            nameplates.put("enabled", worldConfig.PLAYER_TRACKER_NAMEPLATE_ENABLED);
            nameplates.put("show_heads", worldConfig.PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD);
            nameplates.put("heads_url", worldConfig.PLAYER_TRACKER_NAMEPLATE_HEADS_URL);
            playerTracker.put("nameplates", nameplates);

            Map<String, Object> zoom = new HashMap<>();
            zoom.put("max", worldConfig.ZOOM_MAX);
            zoom.put("def", worldConfig.ZOOM_DEFAULT);
            zoom.put("extra", worldConfig.ZOOM_EXTRA);

            Map<String, Object> settings = new HashMap<>();
            settings.put("spawn", spawn);
            settings.put("player_tracker", playerTracker);
            settings.put("zoom", zoom);

            String name = world.getName();
            String displayName = worldConfig.MAP_DISPLAY_NAME
                    .replace("{world}", name);
            String type = world.getEnvironment().name().toLowerCase();

            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("display_name", displayName);
            map.put("type", type);
            map.put("settings", settings);

            Map<String, Object> worldsList = new HashMap<>();
            worldsList.put("name", name);
            worldsList.put("display_name", displayName);
            worldsList.put("type", type);
            worlds.add(worldsList);

            FileUtil.write(gson.toJson(map), FileUtil.getWorldFolder(world).resolve("settings.json"));
        });

        Map<String, Object> ui = new HashMap<>();
        ui.put("title", Config.UI_TITLE);
        ui.put("coordinates", Config.UI_COORDINATES);
        ui.put("link", Config.UI_LINK);
        ui.put("sidebar", Config.UI_SIDEBAR);

        Map<String, Object> map = new HashMap<>();
        map.put("worlds", worlds);
        map.put("ui", ui);

        FileUtil.write(gson.toJson(map), FileUtil.TILES_DIR.resolve("settings.json"));
    }
}
