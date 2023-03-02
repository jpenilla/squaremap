package xyz.jpenilla.squaremap.forge.network;

import com.google.inject.Inject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.network.NetworkingHandler;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public final class ForgeNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final EventNetworkChannel CHANNEL = NetworkRegistry.newEventChannel(
        NetworkingHandler.CHANNEL,
        () -> PROTOCOL_VERSION,
        NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
        NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    private final NetworkingHandler networking;

    @Inject
    private ForgeNetworking(final NetworkingHandler networking) {
        this.networking = networking;
    }

    public void register() {
        CHANNEL.addListener((NetworkEvent.ClientCustomPayloadEvent event) -> {
            final @Nullable ServerPlayer player = event.getSource().get().getSender();
            if (player == null) {
                return;
            }
            event.getSource().get().setPacketHandled(true);
            this.networking.handleIncoming(player, Util.raw(event.getPayload()), map -> true);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> this.networking.onDisconnect(event.getEntity().getUUID()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            this.networking.worldChanged(player);
        });
    }
}
