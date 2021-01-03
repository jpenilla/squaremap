package net.pl3x.map.task;

import com.google.gson.Gson;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.configuration.WorldConfig;
import net.pl3x.map.util.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

public class UpdateWorldData extends BukkitRunnable {
    private final Gson gson = new Gson();

    @Override
    public void run() {
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
            playerTracker.put("nameplates", nameplates);

            Map<String, Object> ui = new HashMap<>();
            ui.put("title", worldConfig.UI_TITLE);
            ui.put("coordinates", worldConfig.UI_COORDINATES);

            Map<String, Object> settings = new HashMap<>();
            settings.put("player_tracker", playerTracker);
            settings.put("ui", ui);

            Map<String, Object> map = new HashMap<>();
            map.put("spawn", spawn);
            map.put("settings", settings);

            Bukkit.getScheduler().runTaskAsynchronously(Pl3xMap.getInstance(), () ->
                    FileUtil.writeStringToFile(gson.toJson(map),
                            new File(FileUtil.getWorldFolder(world),
                                    "settings.json"))
            );
        });
    }
}
