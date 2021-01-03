package net.pl3x.map.task;

import com.google.gson.Gson;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.configuration.WorldConfig;
import net.pl3x.map.util.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

public class UpdatePlayers extends BukkitRunnable {
    private final Gson gson = new Gson();

    @Override
    public void run() {
        Bukkit.getWorlds().forEach(world -> {
            WorldConfig worldConfig = WorldConfig.get(world);

            List<Object> players = new ArrayList<>();

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
                players.add(playerEntry);
            });

            Map<String, Object> map = new HashMap<>();
            map.put("players", players);

            Bukkit.getScheduler().runTaskAsynchronously(Pl3xMap.getInstance(), () ->
                    FileUtil.writeStringToFile(gson.toJson(map),
                            new File(FileUtil.getWorldFolder(world),
                                    "players.json"))
            );
        });
    }
}
