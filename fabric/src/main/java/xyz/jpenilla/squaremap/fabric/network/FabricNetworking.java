package xyz.jpenilla.squaremap.fabric.network;

import com.google.inject.Inject;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.network.NetworkingHandler;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.fabric.event.ServerPlayerEvents;

@DefaultQualifier(NonNull.class)
public final class FabricNetworking {
    private final NetworkingHandler networking;

    @Inject
    private FabricNetworking(final NetworkingHandler networking) {
        this.networking = networking;
    }

    public void register() {
        // ServerPlayNetworking.registerGlobalReceiver(NetworkingHandler.CHANNEL, this::handleInconming); // TODO 1.20.5
        ServerPlayConnectionEvents.DISCONNECT.register(this::handleDisconnect);
        ServerPlayerEvents.WORLD_CHANGED.register(this.networking::worldChanged);
    }

    private void handleInconming(
        final MinecraftServer server,
        final ServerPlayer player,
        final ServerGamePacketListenerImpl handler,
        final FriendlyByteBuf buf,
        final PacketSender responseSender
    ) {
        this.networking.handleIncoming(player, Util.raw(buf), map -> true);
    }

    private void handleDisconnect(
        final ServerGamePacketListenerImpl handler,
        final MinecraftServer server
    ) {
        this.networking.onDisconnect(handler.player.getUUID());
    }
}
