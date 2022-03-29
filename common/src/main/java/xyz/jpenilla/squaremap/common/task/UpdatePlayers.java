package xyz.jpenilla.squaremap.common.task;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.FileUtil;
import xyz.jpenilla.squaremap.common.util.HtmlComponentSerializer;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public class UpdatePlayers implements Runnable {
    private static final Gson GSON = new Gson();

    private final Provider<ComponentFlattener> flattener;
    private final AbstractPlayerManager playerManager;
    private final DirectoryProvider directoryProvider;
    private final ServerAccess serverAccess;

    @Inject
    private UpdatePlayers(
        final Provider<ComponentFlattener> flattener,
        final AbstractPlayerManager playerManager,
        final DirectoryProvider directoryProvider,
        final ServerAccess serverAccess
    ) {
        this.flattener = flattener;
        this.playerManager = playerManager;
        this.directoryProvider = directoryProvider;
        this.serverAccess = serverAccess;
    }

    @Override
    public void run() {
        List<Object> players = new ArrayList<>();

        final HtmlComponentSerializer htmlComponentSerializer = HtmlComponentSerializer.withFlattener(this.flattener.get());

        this.serverAccess.levels().forEach(world -> {
            final WorldConfig worldConfig = WorldConfig.get(world);

            world.players().forEach(player -> {
                if (worldConfig.PLAYER_TRACKER_HIDE_SPECTATORS && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    return;
                }
                if (worldConfig.PLAYER_TRACKER_HIDE_INVISIBLE && player.isInvisible()) {
                    return;
                }
                if (this.playerManager.hidden(player) || this.playerManager.otherwiseHidden(player)) {
                    return;
                }
                final Map<String, Object> playerEntry = new HashMap<>();
                final Vec3 playerLoc = player.position();
                playerEntry.put("name", player.getGameProfile().getName());
                if (worldConfig.PLAYER_TRACKER_USE_DISPLAY_NAME) {
                    playerEntry.put("display_name", htmlComponentSerializer.serialize(this.playerManager.displayName(player)));
                }
                playerEntry.put("uuid", player.getUUID().toString().replace("-", ""));
                playerEntry.put("world", Util.levelWebName(world));
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

        final Map<String, Object> map = new HashMap<>();
        map.put("players", players);
        map.put("max", this.serverAccess.maxPlayers());

        FileUtil.writeStringAsync(this.directoryProvider.tilesDirectory().resolve("players.json"), () -> GSON.toJson(map));
    }

    private static int armorPoints(final ServerPlayer player) {
        final @Nullable AttributeInstance attribute = player.getAttribute(Attributes.ARMOR);
        return attribute == null ? 0 : (int) attribute.getValue();
    }
}
