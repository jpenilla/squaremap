package xyz.jpenilla.squaremap.paper.listener;

import net.minecraft.server.level.ServerLevel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.paper.SquaremapPaper;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

@DefaultQualifier(NonNull.class)
public record WorldLoadListener(SquaremapPaper plugin) implements Listener {
    // Use low priority to load world before other plugins load listeners
    @EventHandler(priority = EventPriority.LOW)
    public void handleWorldLoad(final WorldLoadEvent event) {
        final ServerLevel level = CraftBukkitReflection.serverLevel(event.getWorld());
        WorldConfig.get(level);
        this.plugin.worldManager().getWorldIfEnabled(level);
    }

    // Use high priority to unload world after other plugins unload listeners
    @EventHandler(priority = EventPriority.HIGH)
    public void handleWorldUnload(final WorldUnloadEvent event) {
        this.plugin.worldManager().worldUnloaded(event.getWorld());
    }
}
