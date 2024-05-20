package xyz.jpenilla.squaremap.sponge.network;

import com.google.inject.Inject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.network.EngineConnectionState;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.ChannelManager;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.profile.GameProfile;
import xyz.jpenilla.squaremap.common.network.NetworkingHandler;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public final class SpongeNetworking {
    private final NetworkingHandler networking;

    @Inject
    private SpongeNetworking(
        final NetworkingHandler networking,
        final ChannelManager channelManager
    ) {
        this.networking = networking;
        channelManager.ofType(
            (ResourceKey) (Object) NetworkingHandler.CHANNEL,
            RawDataChannel.class
        ).play().addHandler(EngineConnectionState.Game.class, this::handleIncoming);
    }

    private void handleIncoming(final ChannelBuf data, final EngineConnectionState.Game state) {
        this.networking.handleIncoming((ServerPlayer) state.player(), Util.raw((FriendlyByteBuf) data), map -> true);
    }

    @Listener
    public void changeWorld(final ChangeEntityWorldEvent.Post event) {
        if (event.entity() instanceof ServerPlayer player) {
            this.networking.worldChanged(player);
        }
    }

    @Listener
    public void disconnect(final ServerSideConnectionEvent.Disconnect event) {
        event.profile().map(GameProfile::uuid).ifPresent(this.networking::onDisconnect);
    }
}
