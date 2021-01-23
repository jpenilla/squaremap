package net.pl3x.map.plugin.listener;

import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.configuration.WorldConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class WorldEventListener implements Listener {

    private final Pl3xMapPlugin plugin;

    public WorldEventListener(final @NonNull Pl3xMapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void handleWorldLoad(final @NonNull WorldLoadEvent event) {
        WorldConfig.load(event.getWorld());
        this.plugin.worldManager().getWorldIfEnabled(event.getWorld());
    }

    @EventHandler
    public void handleWorldUnload(final @NonNull WorldUnloadEvent event) {
        this.plugin.worldManager().worldUnloaded(event.getWorld());
    }

}
