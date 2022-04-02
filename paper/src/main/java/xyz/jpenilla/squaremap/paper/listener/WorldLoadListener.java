package xyz.jpenilla.squaremap.paper.listener;

import com.google.inject.Inject;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.paper.SquaremapPaper;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;
import xyz.jpenilla.squaremap.paper.util.WorldNameToKeyMigration;

@DefaultQualifier(NonNull.class)
public final class WorldLoadListener implements Listener {
    private final SquaremapPaper plugin;
    private final DirectoryProvider directoryProvider;

    @Inject
    private WorldLoadListener(
        final SquaremapPaper plugin,
        final DirectoryProvider directoryProvider
    ) {
        this.plugin = plugin;
        this.directoryProvider = directoryProvider;
    }

    // Use low priority to load world before other plugins load listeners
    @EventHandler(priority = EventPriority.LOW)
    public void handleWorldLoad(final WorldLoadEvent event) {
        final ServerLevel level = CraftBukkitReflection.serverLevel(event.getWorld());
        WorldNameToKeyMigration.tryMoveDirectories(this.directoryProvider, level);
        WorldConfig.get(level);
        this.plugin.worldManager().getWorldIfEnabled(level);
    }

    // Use high priority to unload world after other plugins unload listeners
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void handleWorldUnload(final WorldUnloadEvent event) {
        this.plugin.worldManager().worldUnloaded(event.getWorld());
    }
}
