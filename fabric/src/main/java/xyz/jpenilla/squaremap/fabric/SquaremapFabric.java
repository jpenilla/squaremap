package xyz.jpenilla.squaremap.fabric;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.inject.SquaremapModulesBuilder;
import xyz.jpenilla.squaremap.common.task.UpdatePlayers;
import xyz.jpenilla.squaremap.common.task.UpdateWorldData;
import xyz.jpenilla.squaremap.fabric.data.FabricMapWorld;
import xyz.jpenilla.squaremap.fabric.inject.module.FabricModule;
import xyz.jpenilla.squaremap.fabric.network.FabricNetworking;
import xyz.jpenilla.squaremap.fabric.util.FabricMapUpdates;

@DefaultQualifier(NonNull.class)
public final class SquaremapFabric implements SquaremapPlatform {
    private final Injector injector;
    private final SquaremapCommon common;
    private final FabricServerAccess serverAccess;
    private final FabricWorldManager worldManager;
    private @Nullable UpdatePlayers updatePlayers;
    private @Nullable UpdateWorldData updateWorldData;

    private SquaremapFabric() {
        this.injector = Guice.createInjector(
            SquaremapModulesBuilder.forPlatform(this)
                .mapWorldFactory(FabricMapWorld.Factory.class)
                .withModule(new FabricModule(this))
                .vanillaChunkSnapshotProvider()
                .vanillaRegionFileDirectoryResolver()
                .build()
        );
        this.common = this.injector.getInstance(SquaremapCommon.class);
        this.common.init();
        this.worldManager = this.injector.getInstance(FabricWorldManager.class);
        this.serverAccess = this.injector.getInstance(FabricServerAccess.class);
        this.registerLifecycleListeners();
        FabricMapUpdates.registerListeners();
        this.injector.getInstance(FabricNetworking.class).register();
    }

    private void registerLifecycleListeners() {
        ServerLifecycleEvents.SERVER_STARTING.register(this.serverAccess::setServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (server.isDedicatedServer()) {
                this.common.shutdown();
            }
            this.serverAccess.clearServer();
        });
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            new ClientLifecycleListeners().register();
        } else {
            ServerLifecycleEvents.SERVER_STARTED.register($ -> this.common.updateCheck());
        }

        ServerWorldEvents.LOAD.register((server, level) -> this.worldManager.initWorld(level));
        ServerWorldEvents.UNLOAD.register((server, level) -> this.worldManager.worldUnloaded(level));

        ServerTickEvents.END_SERVER_TICK.register(new TickEndListener());
    }

    @Override
    public void startCallback() {
        this.updatePlayers = this.injector.getInstance(UpdatePlayers.class);
        this.updateWorldData = this.injector.getInstance(UpdateWorldData.class);
    }

    @Override
    public void stopCallback() {
        this.updatePlayers = null;
        this.updateWorldData = null;
    }

    @Override
    public String version() {
        return FabricLoader.getInstance().getModContainer("squaremap")
            .orElseThrow().getMetadata().getVersion().getFriendlyString();
    }

    private final class TickEndListener implements ServerTickEvents.EndTick {
        private long tick = 0;

        @Override
        public void onEndTick(final MinecraftServer server) {
            if (this.tick % 20 == 0) {
                if (this.tick % 100 == 0) {
                    if (SquaremapFabric.this.updateWorldData != null) {
                        SquaremapFabric.this.updateWorldData.run();
                    }
                }

                if (SquaremapFabric.this.updatePlayers != null) {
                    SquaremapFabric.this.updatePlayers.run();
                }

                for (final MapWorldInternal mapWorld : SquaremapFabric.this.worldManager.worlds()) {
                    ((FabricMapWorld) mapWorld).tickEachSecond(this.tick);
                }
            }

            this.tick++;
        }
    }

    // this must be a separate class to SquaremapFabric to avoid attempting to load client
    // classes on the server when guice scans for methods
    private final class ClientLifecycleListeners {
        void register() {
            ClientLifecycleEvents.CLIENT_STARTED.register($ -> SquaremapFabric.this.common.updateCheck());
            ClientLifecycleEvents.CLIENT_STOPPING.register($ -> SquaremapFabric.this.common.shutdown());
        }
    }

    public static final class Initializer implements ModInitializer {
        @Override
        public void onInitialize() {
            new SquaremapFabric();
        }
    }
}
