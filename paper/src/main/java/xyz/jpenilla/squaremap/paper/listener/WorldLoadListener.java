package xyz.jpenilla.squaremap.paper.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.paper.SquaremapPlugin;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

@DefaultQualifier(NonNull.class)
public record WorldLoadListener(SquaremapPlugin plugin) implements Listener {
    // Use low priority to load world before other plugins load listeners
    @EventHandler(priority = EventPriority.LOW)
    public void handleWorldLoad(final WorldLoadEvent event) {
        WorldConfig.get(CraftBukkitReflection.serverLevel(event.getWorld()));
        this.plugin.worldManager().getWorldIfEnabled(event.getWorld());
    }

    // Use high priority to unload world after other plugins unload listeners
    @EventHandler(priority = EventPriority.HIGH)
    public void handleWorldUnload(final WorldUnloadEvent event) {
        this.plugin.worldManager().worldUnloaded(event.getWorld());
    }
}
