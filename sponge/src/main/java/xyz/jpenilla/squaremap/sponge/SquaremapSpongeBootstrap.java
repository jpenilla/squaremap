package xyz.jpenilla.squaremap.sponge;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterChannelEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import xyz.jpenilla.squaremap.common.inject.SquaremapModulesBuilder;
import xyz.jpenilla.squaremap.common.network.NetworkingHandler;
import xyz.jpenilla.squaremap.sponge.data.SpongeMapWorld;
import xyz.jpenilla.squaremap.sponge.inject.module.SpongeModule;

@DefaultQualifier(NonNull.class)
@Plugin("squaremap")
public final class SquaremapSpongeBootstrap {
    public static SquaremapSpongeBootstrap instance;

    private final SpongeModule spongeModule;

    @Inject
    public SquaremapSpongeBootstrap(final SpongeModule spongeModule) {
        instance = this;
        this.spongeModule = spongeModule;
    }

    public void init() {
        final Injector injector = Guice.createInjector(
            SquaremapModulesBuilder.forPlatform(SquaremapSponge.class)
                .mapWorld(SpongeMapWorld.class)
                .withModule(this.spongeModule)
                .vanillaChunkSnapshotProviderFactory()
                .vanillaRegionFileDirectoryResolver()
                .build()
        );
        injector.getInstance(SquaremapSponge.class).init();
    }

    @Listener
    public void channelRegistration(final RegisterChannelEvent event) {
        final ResourceKey key = (ResourceKey) (Object) NetworkingHandler.CHANNEL;
        event.register(key, RawDataChannel.class);
    }

    @Listener
    public void registerData(final RegisterDataEvent event) {
        event.register(DataRegistration.of(SpongePlayerManager.HIDDEN_KEY, ServerPlayer.class));
    }
}
