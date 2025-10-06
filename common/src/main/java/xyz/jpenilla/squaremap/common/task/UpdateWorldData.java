package xyz.jpenilla.squaremap.common.task;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.WorldManager;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.FileUtil;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public final class UpdateWorldData implements Runnable {
    private final WorldManager worldManager;
    private final DirectoryProvider directoryProvider;
    private @MonotonicNonNull Map<WorldIdentifier, ChangingData> lastUpdate = null;

    @Inject
    private UpdateWorldData(
        final WorldManager worldManager,
        final DirectoryProvider directoryProvider
    ) {
        this.worldManager = worldManager;
        this.directoryProvider = directoryProvider;
    }

    @Override
    public void run() {
        if (this.lastUpdate != null) {
            final Map<WorldIdentifier, ChangingData> current = this.worldManager.worlds().stream()
                .collect(Collectors.toMap(MapWorld::identifier, ChangingData::snapshot));
            if (this.lastUpdate.equals(current)) {
                return;
            }
            this.lastUpdate.clear();
        } else {
            this.lastUpdate = new HashMap<>();
        }

        List<Object> worlds = new ArrayList<>();

        this.worldManager.worlds().forEach(mapWorld -> {
            this.lastUpdate.put(mapWorld.identifier(), ChangingData.snapshot(mapWorld));

            final ServerLevel level = mapWorld.serverLevel();
            final WorldConfig worldConfig = mapWorld.config();

            this.writeWorldSettings(mapWorld, level, worldConfig);

            final Map<String, Object> worldsList = new HashMap<>();
            //worldsList.put("name", world.getName());
            worldsList.put("name", Util.levelWebName(level));
            worldsList.put("display_name", worldConfig.MAP_DISPLAY_NAME
                .replace("{world}", Util.levelConfigName(level)));
            //.replace("{world}", world.getName()));
            worldsList.put("icon", worldConfig.MAP_ICON);
            //worldsList.put("type", world.getEnvironment().name().toLowerCase());
            worldsList.put("type", environment(level));
            worldsList.put("order", worldConfig.MAP_ORDER);
            worlds.add(worldsList);
        });

        final Map<String, Object> ui = new HashMap<>();
        ui.put("title", Messages.UI_TITLE);

        final Map<String, Object> coordinates = new HashMap<>();
        coordinates.put("enabled", Config.UI_COORDINATES_ENABLED);
        coordinates.put("html", Messages.UI_COORDINATES_HTML);
        ui.put("coordinates", coordinates);

        final Map<String, Object> link = new HashMap<>();
        link.put("enabled", Config.UI_LINK_ENABLED);
        ui.put("link", link);

        final Map<String, Object> sidebar = new HashMap<>();
        sidebar.put("pinned", Config.UI_SIDEBAR_PINNED);
        sidebar.put("player_list_label", Messages.UI_SIDEBAR_PLAYER_LIST_LABEL);
        sidebar.put("world_list_label", Messages.UI_SIDEBAR_WORLD_LIST_LABEL);
        ui.put("sidebar", sidebar);

        final Map<String, Object> map = new HashMap<>();
        map.put("worlds", worlds);
        map.put("ui", ui);

        FileUtil.atomicWriteJsonAsync(this.directoryProvider.tilesDirectory().resolve("settings.json"), map);
    }

    private void writeWorldSettings(
        final MapWorldInternal mapWorld,
        final ServerLevel level,
        final WorldConfig worldConfig
    ) {
        final Map<String, Object> spawn = new HashMap<>();
        final BlockPos loc = mapWorld.getAnySpawnPos();
        spawn.put("x", loc.getX());
        spawn.put("z", loc.getZ());

        final Map<String, Object> playerTracker = new HashMap<>();
        playerTracker.put("enabled", worldConfig.PLAYER_TRACKER_ENABLED);
        playerTracker.put("update_interval", worldConfig.PLAYER_TRACKER_UPDATE_INTERVAL);
        playerTracker.put("label", Messages.UI_PLAYER_TRACKER_LABEL);
        playerTracker.put("show_controls", worldConfig.PLAYER_TRACKER_SHOW_CONTROLS);
        playerTracker.put("default_hidden", worldConfig.PLAYER_TRACKER_DEFAULT_HIDDEN);
        playerTracker.put("priority", worldConfig.PLAYER_TRACKER_PRIORITY);
        playerTracker.put("z_index", worldConfig.PLAYER_TRACKER_Z_INDEX);

        final Map<String, Object> nameplates = new HashMap<>();
        nameplates.put("enabled", worldConfig.PLAYER_TRACKER_NAMEPLATE_ENABLED);
        nameplates.put("show_heads", worldConfig.PLAYER_TRACKER_NAMEPLATE_SHOW_HEAD);
        nameplates.put("heads_url", worldConfig.PLAYER_TRACKER_NAMEPLATE_HEADS_URL);
        nameplates.put("show_armor", worldConfig.PLAYER_TRACKER_NAMEPLATE_SHOW_ARMOR);
        nameplates.put("show_health", worldConfig.PLAYER_TRACKER_NAMEPLATE_SHOW_HEALTH);
        playerTracker.put("nameplates", nameplates);

        final Map<String, Object> zoom = new HashMap<>();
        zoom.put("max", worldConfig.ZOOM_MAX);
        zoom.put("def", worldConfig.ZOOM_DEFAULT);
        zoom.put("extra", worldConfig.ZOOM_EXTRA);

        final Map<String, Object> settings = new HashMap<>();
        settings.put("spawn", spawn);
        settings.put("player_tracker", playerTracker);
        settings.put("zoom", zoom);
        settings.put("marker_update_interval", worldConfig.MARKER_API_UPDATE_INTERVAL_SECONDS);
        settings.put("tiles_update_interval", worldConfig.BACKGROUND_RENDER_INTERVAL_SECONDS);

        FileUtil.atomicWriteJsonAsync(mapWorld.tilesPath().resolve("settings.json"), settings);
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

    private record ChangingData(BlockPos spawn, long lastReset) {
        static ChangingData snapshot(final MapWorldInternal world) {
            return new ChangingData(world.getAnySpawnPos(), world.lastReset());
        }
    }
}
