package xyz.jpenilla.squaremap.forge;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.WorldManagerImpl;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.inject.SquaremapModulesBuilder;
import xyz.jpenilla.squaremap.common.task.UpdatePlayers;
import xyz.jpenilla.squaremap.common.task.UpdateWorldData;
import xyz.jpenilla.squaremap.forge.data.ForgeMapWorld;
import xyz.jpenilla.squaremap.forge.event.ForgeMapUpdates;
import xyz.jpenilla.squaremap.forge.inject.module.ForgeModule;
import xyz.jpenilla.squaremap.forge.network.ForgeNetworking;

@DefaultQualifier(NonNull.class)
@Mod("squaremap")
public final class SquaremapForge implements SquaremapPlatform {
    private final Injector injector;
    private final SquaremapCommon common;
    private final ForgeServerAccess serverAccess;
    private final WorldManagerImpl worldManager;
    private final ModContainer container;
    private @Nullable UpdatePlayers updatePlayers;
    private @Nullable UpdateWorldData updateWorldData;

    public SquaremapForge(final IEventBus modEventBus, final ModContainer modContainer) {
        this.injector = Guice.createInjector(
            SquaremapModulesBuilder.forPlatform(this)
                .mapWorld(ForgeMapWorld.class)
                .withModule(new ForgeModule(this, modEventBus, modContainer))
                .vanillaChunkSnapshotProviderFactory()
                .vanillaRegionFileDirectoryResolver()
                .squaremapJarAccess(ForgeSquaremapJarAccess.class)
                .build()
        );
        this.common = this.injector.getInstance(SquaremapCommon.class);
        this.common.init();
        this.worldManager = this.injector.getInstance(WorldManagerImpl.class);
        this.serverAccess = this.injector.getInstance(ForgeServerAccess.class);
        this.container = this.injector.getInstance(ModContainer.class);
        this.registerLifecycleListeners();
        this.injector.getInstance(ForgeMapUpdates.class).register();
        this.injector.getInstance(ForgeNetworking.class).register();
        this.injector.getInstance(ForgePlayerManager.class).setupCapabilities();
    }

    private void registerLifecycleListeners() {
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> this.serverAccess.setServer(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> this.serverAccess.clearServer());
        final AtomicBoolean exportedFluids = new AtomicBoolean(false);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> exportedFluids.set(false));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (LevelEvent.Load event) -> {
            if (event.getLevel().isClientSide() && !exportedFluids.getAndSet(true)) {
                this.injector.getInstance(ForgeFluidColorExporter.class).export(event.getLevel().registryAccess());
            }
            if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
                return;
            }
            this.worldManager.initWorld(serverLevel);
        });
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
                return;
            }
            this.worldManager.worldUnloaded(serverLevel);
        });
        NeoForge.EVENT_BUS.addListener(new TickEndListener());
        NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> this.common.shutdown());
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> this.common.updateCheck());
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
        return this.container.getModInfo().getVersion().toString();
    }

    @Override
    public boolean hasMod(final String id) {
        return ModList.get().isLoaded(id);
    }

    private final class TickEndListener implements Consumer<ServerTickEvent.Post> {
        private long tick = 0;

        @Override
        public void accept(final ServerTickEvent.Post event) {
            if (this.tick % 20 == 0) {
                if (this.tick % 100 == 0) {
                    if (SquaremapForge.this.updateWorldData != null) {
                        SquaremapForge.this.updateWorldData.run();
                    }
                }

                if (SquaremapForge.this.updatePlayers != null) {
                    SquaremapForge.this.updatePlayers.run();
                }

                for (final MapWorldInternal mapWorld : SquaremapForge.this.worldManager.worlds()) {
                    ((ForgeMapWorld) mapWorld).tickEachSecond(this.tick);
                }
            }

            this.tick++;
        }
    }
}
