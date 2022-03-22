package xyz.jpenilla.squaremap.paper.network;

import com.google.inject.Inject;
import net.minecraft.server.level.ServerPlayer;
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
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.network.Networking;
import xyz.jpenilla.squaremap.paper.SquaremapPaper;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

@DefaultQualifier(NonNull.class)
public final class PaperNetworking implements Listener {
    private final SquaremapPaper plugin;
    private final ServerAccess serverAccess;
    private final Server server;

    @Inject
    private PaperNetworking(
        final SquaremapPaper plugin,
        final ServerAccess serverAccess,
        final Server server
    ) {
        this.plugin = plugin;
        this.serverAccess = serverAccess;
        this.server = server;
    }

    public void register() {
        this.server.getPluginManager().registerEvents(this, this.plugin);
        this.server.getMessenger().registerIncomingPluginChannel(
            this.plugin,
            Networking.CHANNEL.toString(),
            this::handlePluginMessage
        );
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        this.server.getMessenger().unregisterIncomingPluginChannel(this.plugin, Networking.CHANNEL.toString());
    }

    private void handlePluginMessage(final String channel, final Player player, final byte[] bytes) {
        final ServerPlayer serverPlayer = CraftBukkitReflection.serverPlayer(player);
        Networking.handleIncoming(
            this.plugin,
            this.serverAccess,
            bytes,
            serverPlayer,
            PaperNetworking::vanillaMap
        );
    }

    @EventHandler
    public void handlePlayerQuit(final PlayerQuitEvent event) {
        Networking.CLIENT_USERS.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void handlePlayerChangedWorld(final PlayerChangedWorldEvent event) {
        Networking.worldChanged(CraftBukkitReflection.serverPlayer(event.getPlayer()));
    }

    private static boolean vanillaMap(final MapItemSavedData mapData) {
        for (final MapRenderer renderer : mapData.mapView.getRenderers()) {
            if (!renderer.getClass().getSimpleName().equals("CraftMapRenderer")) {
                return false;
            }
        }
        return true;
    }
}
