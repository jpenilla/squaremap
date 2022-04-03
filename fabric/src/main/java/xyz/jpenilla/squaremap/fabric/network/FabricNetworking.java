package xyz.jpenilla.squaremap.fabric.network;

import com.google.inject.Inject;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.WorldManager;
import xyz.jpenilla.squaremap.common.network.Networking;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public final class FabricNetworking {
    private final WorldManager worldManager;
    private final ServerAccess serverAccess;

    @Inject
    private FabricNetworking(
        final WorldManager worldManager,
        final ServerAccess serverAccess
    ) {
        this.worldManager = worldManager;
        this.serverAccess = serverAccess;
    }

    public void register() {
        ServerPlayNetworking.registerGlobalReceiver(
            Networking.CHANNEL,
            (final MinecraftServer server,
             final ServerPlayer player,
             final ServerGamePacketListenerImpl handler,
             final FriendlyByteBuf buf,
             final PacketSender responseSender) ->
                Networking.handleIncoming(this.worldManager, this.serverAccess, Util.raw(buf), player, $ -> true)
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            Networking.CLIENT_USERS.remove(handler.player.getUUID()));
    }
}
