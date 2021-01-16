package net.pl3x.map.plugin.task;

import com.google.gson.Gson;
import net.pl3x.map.plugin.configuration.WorldConfig;
import net.pl3x.map.plugin.util.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdatePlayers extends BukkitRunnable {
    private final Gson gson = new Gson();

    @Override
    public void run() {
        List<Object> players = new ArrayList<>();

        Bukkit.getWorlds().forEach(world -> {
            WorldConfig worldConfig = WorldConfig.get(world);

            world.getPlayers().forEach(player -> {
                if (worldConfig.PLAYER_TRACKER_HIDE_SPECTATORS && player.getGameMode() == GameMode.SPECTATOR) {
                    return;
                }
                if (worldConfig.PLAYER_TRACKER_HIDE_INVISIBLE && player.isInvisible()) {
                    return;
                }
                Map<String, Object> playerEntry = new HashMap<>();
                Location playerLoc = player.getLocation();
                playerEntry.put("name", player.getName());
                playerEntry.put("uuid", player.getUniqueId());
                playerEntry.put("x", playerLoc.getBlockX());
                playerEntry.put("z", playerLoc.getBlockZ());
                playerEntry.put("yaw", playerLoc.getYaw());
                playerEntry.put("world", playerLoc.getWorld().getName());
                players.add(playerEntry);
            });
        });

        Map<String, Object> map = new HashMap<>();
        map.put("players", players);

        FileUtil.write(gson.toJson(map), FileUtil.TILES_DIR.resolve("players.json"));
    }
}
