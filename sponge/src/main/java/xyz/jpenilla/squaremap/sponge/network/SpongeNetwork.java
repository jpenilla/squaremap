package xyz.jpenilla.squaremap.sponge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.lifecycle.RegisterChannelEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.network.EngineConnectionSide;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import xyz.jpenilla.squaremap.common.network.Networking;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public final class SpongeNetwork {
    @Listener
    public void channelRegistration(final RegisterChannelEvent event) {
        final RawDataChannel channel = event.register(
            (ResourceKey) (Object) Networking.CHANNEL,
            RawDataChannel.class
        );
        channel.play().addHandler(
            EngineConnectionSide.SERVER,
            (sponge, connection) -> {
                if (!(connection instanceof ServerGamePacketListenerImpl listener)) {
                    return;
                }
                Networking.handleIncoming(
                    Util.raw((FriendlyByteBuf) sponge),
                    listener.getPlayer(),
                    $ -> true
                );
            }
        );
    }

    @Listener
    public void changeWorld(final ChangeEntityWorldEvent.Post event) {
        if (event.entity() instanceof ServerPlayer player) {
            Networking.worldChanged(player);
        }
    }

    @Listener
    public void disconnect(final ServerSideConnectionEvent.Disconnect event) {
        Networking.CLIENT_USERS.remove(event.player().uniqueId());
    }
}
