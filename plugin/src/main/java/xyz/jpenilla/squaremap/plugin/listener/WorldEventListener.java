package xyz.jpenilla.squaremap.plugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.config.WorldConfig;

public final class WorldEventListener implements Listener {

    private final SquaremapPlugin plugin;

    public WorldEventListener(final @NonNull SquaremapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void handleWorldLoad(final @NonNull WorldLoadEvent event) {
        WorldConfig.get(event.getWorld());
        this.plugin.worldManager().getWorldIfEnabled(event.getWorld());
    }

    @EventHandler
    public void handleWorldUnload(final @NonNull WorldUnloadEvent event) {
        this.plugin.worldManager().worldUnloaded(event.getWorld());
    }

}
