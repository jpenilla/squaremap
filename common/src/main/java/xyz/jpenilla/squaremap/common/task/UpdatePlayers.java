package xyz.jpenilla.squaremap.common.task;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
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
import xyz.jpenilla.squaremap.api.HtmlComponentSerializer;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.httpd.JsonCache;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public final class UpdatePlayers implements Runnable {
    private static final String JSON_PATH = "/tiles/players.json";

    private final Provider<ComponentFlattener> flattener;
    private final AbstractPlayerManager playerManager;
    private final ServerAccess serverAccess;
    private final ConfigManager configManager;
    private final JsonCache jsonCache;
    private @Nullable Map<String, Object> lastData = null;

    @Inject
    private UpdatePlayers(
        final Provider<ComponentFlattener> flattener,
        final AbstractPlayerManager playerManager,
        final ServerAccess serverAccess,
        final ConfigManager configManager,
        final JsonCache jsonCache
    ) {
        this.flattener = flattener;
        this.playerManager = playerManager;
        this.serverAccess = serverAccess;
        this.configManager = configManager;
        this.jsonCache = jsonCache;
    }

    @Override
    public void run() {
        final @Nullable Map<String, Object> prev = this.lastData;
        final Map<String, Object> data = this.collectData();
        this.lastData = data;

        ForkJoinPool.commonPool().execute(() -> {
            if (prev == null || !prev.equals(data)) {
                final String json = Util.gson().toJson(data);
                this.jsonCache.put(JSON_PATH, json);
            }
        });
    }

    private Map<String, Object> collectData() {
        final List<Object> players = new ArrayList<>();

        final HtmlComponentSerializer htmlComponentSerializer = HtmlComponentSerializer.withFlattener(this.flattener.get());

        this.serverAccess.levels().forEach(world -> {
            final WorldConfig worldConfig = this.configManager.worldConfig(world);

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

        return map;
    }

    private static int armorPoints(final ServerPlayer player) {
        final @Nullable AttributeInstance attribute = player.getAttribute(Attributes.ARMOR);
        return attribute == null ? 0 : (int) attribute.getValue();
    }
}
