package xyz.jpenilla.squaremap.forge.network;

import com.google.inject.Inject;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.network.NetworkingHandler;

@DefaultQualifier(NonNull.class)
public final class ForgeNetworking {
    private final NetworkingHandler networking;

    @Inject
    private ForgeNetworking(final NetworkingHandler networking) {
        this.networking = networking;
    }

    public void register() {
        /* TODO 1.20.5
        NeoForge.EVENT_BUS.addListener((RegisterPayloadHandlerEvent event) -> {
            final IPayloadRegistrar registrar = event.registrar("squaremap");
            registrar.play(
                NetworkingHandler.CHANNEL,
                fbb -> new NetworkingHandler.SquaremapClientPayload(Util.raw(fbb)),
                builder -> {
                    builder.server((payload, context) -> {
                        context.player().ifPresent(player -> {
                            if (!(player instanceof ServerPlayer serverPlayer)) {
                                return;
                            }
                            this.networking.handleIncoming(serverPlayer, payload.bytes(), map -> true);
                        });
                    });
                }
            ).optional();
        });
         */
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> this.networking.onDisconnect(event.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            this.networking.worldChanged(player);
        });
    }
}
