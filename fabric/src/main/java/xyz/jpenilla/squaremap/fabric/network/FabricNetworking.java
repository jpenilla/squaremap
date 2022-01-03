package xyz.jpenilla.squaremap.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import xyz.jpenilla.squaremap.common.network.Networking;

public final class FabricNetworking {
    private FabricNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
            Networking.CHANNEL,
            (final MinecraftServer server,
             final ServerPlayer player,
             final ServerGamePacketListenerImpl handler,
             final FriendlyByteBuf buf,
             final PacketSender responseSender) -> {
                final byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                Networking.handleIncoming(data, player, $ -> true);
            }
        );
    }
}
