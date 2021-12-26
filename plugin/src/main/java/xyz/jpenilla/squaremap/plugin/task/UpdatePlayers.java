package xyz.jpenilla.squaremap.plugin.task;

import com.google.gson.Gson;
import io.papermc.paper.text.PaperComponents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.config.WorldConfig;
import xyz.jpenilla.squaremap.plugin.util.FileUtil;
import xyz.jpenilla.squaremap.plugin.util.HtmlComponentSerializer;

public class UpdatePlayers extends BukkitRunnable {
    private final SquaremapPlugin plugin;
    private final Gson gson = new Gson();
    private final HtmlComponentSerializer htmlComponentSerializer = HtmlComponentSerializer.withFlattener(PaperComponents.flattener());

    public UpdatePlayers(final SquaremapPlugin plugin) {
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
                if (this.plugin.playerManager().hidden(player)) {
                    return;
                }
                if (player.hasMetadata("NPC")) {
                    return;
                }
                Map<String, Object> playerEntry = new HashMap<>();
                Location playerLoc = player.getLocation();
                playerEntry.put("name", player.getName());
                if (worldConfig.PLAYER_TRACKER_USE_DISPLAY_NAME) {
                    playerEntry.put("display_name", this.htmlComponentSerializer.serialize(player.displayName()));
                }
                playerEntry.put("uuid", player.getUniqueId().toString().replace("-", ""));
                playerEntry.put("world", playerLoc.getWorld().getName());
                if (worldConfig.PLAYER_TRACKER_ENABLED) {
                    playerEntry.put("x", playerLoc.getBlockX());
                    playerEntry.put("z", playerLoc.getBlockZ());
                    playerEntry.put("yaw", playerLoc.getYaw());
                    if (worldConfig.PLAYER_TRACKER_NAMEPLATE_SHOW_ARMOR) {
                        playerEntry.put("armor", getArmorPoints(player));
                    }
                    if (worldConfig.PLAYER_TRACKER_NAMEPLATE_SHOW_HEALTH) {
                        playerEntry.put("health", (int) player.getHealth());
                    }
                }
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
