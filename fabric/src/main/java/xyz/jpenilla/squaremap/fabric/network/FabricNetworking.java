package xyz.jpenilla.squaremap.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.network.Networking;
import xyz.jpenilla.squaremap.common.util.Util;

public final class FabricNetworking {
    private FabricNetworking() {
    }

    public static void register(final SquaremapPlatform platform) {
        ServerPlayNetworking.registerGlobalReceiver(
            Networking.CHANNEL,
            (final MinecraftServer server,
             final ServerPlayer player,
             final ServerGamePacketListenerImpl handler,
             final FriendlyByteBuf buf,
             final PacketSender responseSender) ->
                Networking.handleIncoming(platform, Util.raw(buf), player, $ -> true)
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            Networking.CLIENT_USERS.remove(handler.player.getUUID()));
    }
}
