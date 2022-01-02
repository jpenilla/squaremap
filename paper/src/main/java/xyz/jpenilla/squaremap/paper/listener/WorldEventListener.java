package xyz.jpenilla.squaremap.paper.listener;

import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.paper.SquaremapPlugin;

public final class WorldEventListener implements Listener {
    private final SquaremapPlugin plugin;

    public WorldEventListener(final @NonNull SquaremapPlugin plugin) {
        this.plugin = plugin;
    }

    // Use low priority to load world before other plugins load listeners
    @EventHandler(priority = EventPriority.LOW)
    public void handleWorldLoad(final @NonNull WorldLoadEvent event) {
        WorldConfig.get(((CraftWorld) event.getWorld()).getHandle());
        this.plugin.worldManager().getWorldIfEnabled(event.getWorld());
    }

    // Use high priority to unload world after other plugins unload listeners
    @EventHandler(priority = EventPriority.HIGH)
    public void handleWorldUnload(final @NonNull WorldUnloadEvent event) {
        this.plugin.worldManager().worldUnloaded(event.getWorld());
    }
}
