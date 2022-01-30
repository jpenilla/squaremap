package xyz.jpenilla.squaremap.common.task;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.util.FileUtil;
import xyz.jpenilla.squaremap.common.util.HtmlComponentSerializer;

public class UpdatePlayers implements Runnable {
    private static final Gson GSON = new Gson();
    private final SquaremapPlatform platform;

    public UpdatePlayers(final SquaremapPlatform platform) {
        this.platform = platform;
    }

    @Override
    public void run() {
        List<Object> players = new ArrayList<>();

        final HtmlComponentSerializer htmlComponentSerializer = HtmlComponentSerializer.withFlattener(this.platform.componentFlattener());

        this.platform.levels().forEach(world -> {
            WorldConfig worldConfig = WorldConfig.get(world);

            world.players().forEach(player -> {
                if (worldConfig.PLAYER_TRACKER_HIDE_SPECTATORS && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    return;
                }
                if (worldConfig.PLAYER_TRACKER_HIDE_INVISIBLE && player.isInvisible()) {
                    return;
                }
                if (this.platform.playerManager().hidden(player) || this.platform.playerManager().otherwiseHidden(player)) {
                    return;
                }
                Map<String, Object> playerEntry = new HashMap<>();
                final Vec3 playerLoc = player.position();
                playerEntry.put("name", player.getGameProfile().getName());
                if (worldConfig.PLAYER_TRACKER_USE_DISPLAY_NAME) {
                    playerEntry.put("display_name", htmlComponentSerializer.serialize(this.platform.playerManager().displayName(player)));
                }
                playerEntry.put("uuid", player.getUUID().toString().replace("-", ""));
                playerEntry.put("world", this.platform.webNameForWorld(world));
                if (worldConfig.PLAYER_TRACKER_ENABLED) {
                    playerEntry.put("x", Mth.floor(playerLoc.x()));
                    playerEntry.put("z", Mth.floor(playerLoc.z()));
                    playerEntry.put("yaw", player.getYHeadRot());
                    if (worldConfig.PLAYER_TRACKER_NAMEPLATE_SHOW_ARMOR) {
                        playerEntry.put("armor", armorPoints(player));
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
        map.put("max", this.platform.maxPlayers());

        FileUtil.writeString(FileUtil.TILES_DIR.resolve("players.json"), () -> GSON.toJson(map));
    }

    private static int armorPoints(final ServerPlayer player) {
        final AttributeInstance attribute = player.getAttribute(Attributes.ARMOR);
        return attribute == null ? 0 : (int) attribute.getValue();
    }
}
