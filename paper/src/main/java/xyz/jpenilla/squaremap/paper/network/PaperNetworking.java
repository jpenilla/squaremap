package xyz.jpenilla.squaremap.paper.network;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;
import xyz.jpenilla.squaremap.common.network.Networking;
import xyz.jpenilla.squaremap.paper.SquaremapPlugin;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

public final class PaperNetworking {
    private PaperNetworking() {
    }

    public static void register(final SquaremapPlugin plugin) {
        Bukkit.getMessenger().registerIncomingPluginChannel(
            plugin,
            Networking.CHANNEL.toString(),
            (channel, player, bytes) -> {
                final ServerPlayer serverPlayer = CraftBukkitReflection.serverPlayer(player);
                Networking.handleIncoming(
                    bytes,
                    serverPlayer,
                    mapData -> {
                        for (final MapRenderer renderer : mapData.mapView.getRenderers()) {
                            if (!renderer.getClass().getSimpleName().equals("CraftMapRenderer")) {
                                return false;
                            }
                        }
                        return true;
                    }
                );
            }
        );
    }

    public static void unregister(final SquaremapPlugin plugin) {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, Networking.CHANNEL.toString());
    }
}
