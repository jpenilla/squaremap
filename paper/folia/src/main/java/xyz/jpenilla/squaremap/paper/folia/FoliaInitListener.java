package xyz.jpenilla.squaremap.paper.folia;

import io.papermc.paper.threadedregions.RegionizedServerInitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class FoliaInitListener implements Listener {
    private final JavaPlugin plugin;
    private final Runnable action;

    public FoliaInitListener(final JavaPlugin plugin, final Runnable action) {
        this.plugin = plugin;
        this.action = action;
    }

    @EventHandler
    public void handle(final RegionizedServerInitEvent event) {
        this.plugin.getServer().getAsyncScheduler().runNow(this.plugin, $ -> this.action.run());
    }
}
