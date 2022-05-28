package xyz.jpenilla.squaremap.paper.network;

import com.google.inject.Inject;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.map.MapRenderer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.network.NetworkingHandler;
import xyz.jpenilla.squaremap.paper.SquaremapPaper;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

@DefaultQualifier(NonNull.class)
public final class PaperNetworking implements Listener {
    private final SquaremapPaper plugin;
    private final Server server;
    private final NetworkingHandler networking;

    @Inject
    private PaperNetworking(
        final SquaremapPaper plugin,
        final Server server,
        final NetworkingHandler networking
    ) {
        this.plugin = plugin;
        this.server = server;
        this.networking = networking;
    }

    public void register() {
        this.server.getPluginManager().registerEvents(this, this.plugin);
        this.server.getMessenger().registerIncomingPluginChannel(
            this.plugin,
            NetworkingHandler.CHANNEL.toString(),
            this::handlePluginMessage
        );
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        this.server.getMessenger().unregisterIncomingPluginChannel(this.plugin, NetworkingHandler.CHANNEL.toString());
    }

    private void handlePluginMessage(final String channel, final Player player, final byte[] data) {
        this.networking.handleIncoming(CraftBukkitReflection.serverPlayer(player), data, PaperNetworking::isVanillaMap);
    }

    @EventHandler
    public void handlePlayerQuit(final PlayerQuitEvent event) {
        this.networking.onDisconnect(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void handlePlayerChangedWorld(final PlayerChangedWorldEvent event) {
        this.networking.worldChanged(CraftBukkitReflection.serverPlayer(event.getPlayer()));
    }

    private static boolean isVanillaMap(final MapItemSavedData mapData) {
        for (final MapRenderer renderer : mapData.mapView.getRenderers()) {
            if (!renderer.getClass().getSimpleName().equals("CraftMapRenderer")) {
                return false;
            }
        }
        return true;
    }
}
