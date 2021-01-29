package net.pl3x.map.plugin.configuration;

import com.google.common.collect.ImmutableSet;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import net.pl3x.map.plugin.Logger;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
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
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

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

    private static final Map<Class<? extends Event>, Boolean> eventListenerToggles = new HashMap<>();

    public static boolean listenerEnabled(final @NonNull Class<? extends Event> eventClass) {
        final Boolean enabled = eventListenerToggles.get(eventClass);
        if (enabled == null) {
            Logger.warn(String.format("No configuration option found for event listener: %s, the listener will not be enabled.", eventClass.getSimpleName()));
            return false;
        }
        return enabled;
    }

    private static void listenerToggles() {
        ImmutableSet.of(
                BlockPlaceEvent.class,
                BlockBreakEvent.class,
                LeavesDecayEvent.class,
                BlockBurnEvent.class,
                BlockExplodeEvent.class,
                BlockGrowEvent.class,
                BlockFormEvent.class,
                EntityBlockFormEvent.class,
                BlockSpreadEvent.class,
                FluidLevelChangeEvent.class,
                EntityExplodeEvent.class,
                EntityChangeBlockEvent.class,
                StructureGrowEvent.class,
                ChunkPopulateEvent.class
        ).forEach(clazz -> eventListenerToggles.put(clazz, config.getBoolean("settings.event-listeners." + clazz.getSimpleName(), true)));

        ImmutableSet.of(
                BlockFromToEvent.class,
                PlayerJoinEvent.class,
                PlayerQuitEvent.class,
                PlayerMoveEvent.class,
                BlockPhysicsEvent.class,
                BlockPistonExtendEvent.class,
                BlockPistonRetractEvent.class
        ).forEach(clazz -> eventListenerToggles.put(clazz, config.getBoolean("settings.event-listeners." + clazz.getSimpleName(), false)));
    }

    public static final Map<Block, Integer> COLOR_OVERRIDES = new HashMap<>();

    private static void colorOverrideSettings() {
        COLOR_OVERRIDES.clear();
        config.getMap("settings.color-overrides", Map.ofEntries(
                Map.entry("minecraft:mycelium", "#6F6265"),
                Map.entry("minecraft:terracotta", "#9E6246")
        )).forEach((key, color) -> {
            final Block block = IRegistry.BLOCK.get(new MinecraftKey(key));
            if (block != Blocks.AIR) {
                COLOR_OVERRIDES.put(block, Color.decode(color).getRGB());
            }
        });
    }

}
