package xyz.jpenilla.squaremap.plugin.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.plugin.Logging;

@SuppressWarnings("unused")
public class Advanced extends AbstractConfig {
    private Advanced() {
        super("advanced.yml");
    }

    static Advanced config;
    static int version;

    public static void reload() {
        config = new Advanced();

        version = config.getInt("config-version", 1);
        config.set("config-version", 1);

        config.readConfig(Advanced.class, null);

        WorldAdvanced.reload();
    }

    private static final Map<Class<? extends Event>, Boolean> EVENT_LISTENER_TOGGLES = new HashMap<>();

    public static boolean listenerEnabled(final @NonNull Class<? extends Event> eventClass) {
        final Boolean enabled = EVENT_LISTENER_TOGGLES.get(eventClass);
        if (enabled == null) {
            Logging.logger().warn(String.format("No configuration option found for event listener: %s, the listener will not be enabled.", eventClass.getSimpleName()));
            return false;
        }
        return enabled;
    }

    private static void listenerToggles() {
        final Set<Class<? extends Event>> defaultOn = Set.of(
            BlockPlaceEvent.class,
            BlockBreakEvent.class,
            LeavesDecayEvent.class,
            BlockBurnEvent.class,
            BlockExplodeEvent.class,
            BlockGrowEvent.class,
            BlockFormEvent.class,
            BlockFadeEvent.class,
            EntityBlockFormEvent.class,
            BlockSpreadEvent.class,
            FluidLevelChangeEvent.class,
            EntityExplodeEvent.class,
            EntityChangeBlockEvent.class,
            StructureGrowEvent.class,
            ChunkPopulateEvent.class
        );
        for (final Class<? extends Event> clazz : defaultOn) {
            EVENT_LISTENER_TOGGLES.put(clazz, config.getBoolean("settings.event-listeners." + clazz.getSimpleName(), true));
        }

        final Set<Class<? extends Event>> defaultOff = Set.of(
            BlockFromToEvent.class,
            PlayerJoinEvent.class,
            PlayerQuitEvent.class,
            PlayerMoveEvent.class,
            BlockPhysicsEvent.class,
            BlockPistonExtendEvent.class,
            BlockPistonRetractEvent.class,
            ChunkLoadEvent.class
        );
        for (final Class<? extends Event> clazz : defaultOff) {
            EVENT_LISTENER_TOGGLES.put(clazz, config.getBoolean("settings.event-listeners." + clazz.getSimpleName(), false));
        }
    }

}
