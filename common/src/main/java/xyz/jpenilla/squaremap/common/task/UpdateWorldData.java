package xyz.jpenilla.squaremap.common.task;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.FileUtil;

@DefaultQualifier(NonNull.class)
public final class UpdateWorldData implements Runnable {
    private static final Gson GSON = new Gson();

    private final SquaremapPlatform platform;

    public UpdateWorldData(final SquaremapPlatform platform) {
        this.platform = platform;
    }

    @Override
    public void run() {
        List<Object> worlds = new ArrayList<>();

        this.platform.levels().forEach(world -> {
            final @Nullable MapWorldInternal mapWorld = this.platform.worldManager().getWorldIfEnabled(world)
                .orElse(null);
            if (mapWorld == null) {
                return;
            }
            final WorldConfig worldConfig = mapWorld.config();

            Map<String, Object> spawn = new HashMap<>();
            final BlockPos loc = world.getSharedSpawnPos();
            spawn.put("x", loc.getX());
            spawn.put("z", loc.getZ());

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

            FileUtil.writeString(mapWorld.tilesPath().resolve("settings.json"), () -> GSON.toJson(settings));

            Map<String, Object> worldsList = new HashMap<>();
            //worldsList.put("name", world.getName());
            worldsList.put("name", this.platform.webNameForWorld(world));
            worldsList.put("display_name", worldConfig.MAP_DISPLAY_NAME
                .replace("{world}", this.platform.configNameForWorld(world)));
            //.replace("{world}", world.getName()));
            worldsList.put("icon", worldConfig.MAP_ICON);
            //worldsList.put("type", world.getEnvironment().name().toLowerCase());
            worldsList.put("type", environment(world));
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

        FileUtil.writeString(FileUtil.TILES_DIR.resolve("settings.json"), () -> GSON.toJson(map));
    }

    // replicate bukkit "environment"
    private static String environment(final ServerLevel level) {
        final ResourceKey<Level> dimensionKey = level.dimension();
        if (dimensionKey == Level.NETHER) {
            return "nether";
        } else if (dimensionKey == Level.END) {
            return "the_end";
        } else if (dimensionKey == Level.OVERWORLD) {
            return "normal";
        }
        return "custom";
    }
}
