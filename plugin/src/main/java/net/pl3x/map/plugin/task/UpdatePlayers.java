package net.pl3x.map.plugin.task;

import com.google.gson.Gson;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.configuration.WorldConfig;
import net.pl3x.map.plugin.util.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdatePlayers extends BukkitRunnable {
    private final Pl3xMapPlugin plugin;
    private final Gson gson = new Gson();

    public UpdatePlayers(Pl3xMapPlugin plugin) {
        this.plugin = plugin;
    }

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
                if (this.plugin.api().playerManager().hidden(player.getUniqueId())) {
                    return;
                }
                Map<String, Object> playerEntry = new HashMap<>();
                Location playerLoc = player.getLocation();
                playerEntry.put("name", player.getName());
                playerEntry.put("uuid", player.getUniqueId().toString().replace("-", ""));
                playerEntry.put("x", worldConfig.PLAYER_TRACKER_ENABLED ? playerLoc.getBlockX() : 0);
                playerEntry.put("z", worldConfig.PLAYER_TRACKER_ENABLED ? playerLoc.getBlockZ() : 0);
                playerEntry.put("yaw", worldConfig.PLAYER_TRACKER_ENABLED ? playerLoc.getYaw() : 0);
                playerEntry.put("world", playerLoc.getWorld().getName());
                playerEntry.put("armor", worldConfig.PLAYER_TRACKER_ENABLED ? getArmorPoints(player) : 0);
                playerEntry.put("health", worldConfig.PLAYER_TRACKER_ENABLED ? (int) player.getHealth() : 0);
                players.add(playerEntry);
            });
        });

        Map<String, Object> map = new HashMap<>();
        map.put("players", players);
        map.put("max", Bukkit.getMaxPlayers());

        FileUtil.write(this.gson.toJson(map), FileUtil.TILES_DIR.resolve("players.json"));
    }

    private static int getArmorPoints(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_ARMOR);
        return attr == null ? 0 : (int) attr.getValue();
    }
}
