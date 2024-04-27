package xyz.jpenilla.squaremap.paper.listener;

import com.google.inject.Inject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.paper.PaperWorldManager;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitHelper;

@DefaultQualifier(NonNull.class)
public final class WorldLoadListener implements Listener {
    private final PaperWorldManager worldManager;

    @Inject
    private WorldLoadListener(final PaperWorldManager worldManager) {
        this.worldManager = worldManager;
    }

    // Use low priority to load world before other plugins load listeners
    @EventHandler(priority = EventPriority.LOW)
    public void handleWorldLoad(final WorldLoadEvent event) {
        this.worldManager.initWorld(CraftBukkitHelper.serverLevel(event.getWorld()));
    }

    // Use high priority to unload world after other plugins unload listeners
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void handleWorldUnload(final WorldUnloadEvent event) {
        this.worldManager.worldUnloaded(CraftBukkitHelper.serverLevel(event.getWorld()));
    }
}
