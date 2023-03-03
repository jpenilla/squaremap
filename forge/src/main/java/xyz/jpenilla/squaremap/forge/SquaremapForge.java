package xyz.jpenilla.squaremap.forge;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
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
    private final ForgeWorldManager worldManager;
    private final ModContainer container;
    private final DirectoryProvider directoryProvider;
    private @Nullable UpdatePlayers updatePlayers;
    private @Nullable UpdateWorldData updateWorldData;

    public SquaremapForge() {
        this.injector = Guice.createInjector(
            SquaremapModulesBuilder.forPlatform(this)
                .mapWorldFactory(ForgeMapWorld.Factory.class)
                .withModule(new ForgeModule(this))
                .vanillaChunkSnapshotProviderFactory()
                .vanillaRegionFileDirectoryResolver()
                .squaremapJarAccess(ForgeSquaremapJarAccess.class)
                .build()
        );
        this.common = this.injector.getInstance(SquaremapCommon.class);
        this.common.init();
        this.worldManager = this.injector.getInstance(ForgeWorldManager.class);
        this.serverAccess = this.injector.getInstance(ForgeServerAccess.class);
        this.container = this.injector.getInstance(ModContainer.class);
        this.directoryProvider = this.injector.getInstance(DirectoryProvider.class);
        this.registerLifecycleListeners();
        this.injector.getInstance(ForgeMapUpdates.class).register();
        this.injector.getInstance(ForgeNetworking.class).register();
        this.injector.getInstance(ForgePlayerManager.class).setupCapabilities();
    }

    private void registerLifecycleListeners() {
        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent event) -> this.serverAccess.setServer(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> this.serverAccess.clearServer());
        final AtomicBoolean exportedFluids = new AtomicBoolean(false);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, (LevelEvent.Load event) -> {
            if (event.getLevel().isClientSide() && !exportedFluids.getAndSet(true)) {
                this.injector.getInstance(FluidColorExporter.class).export(event.getLevel().registryAccess(), this.directoryProvider.dataDirectory().resolve("fluids-export.yml"));
            }
            if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
                return;
            }
            this.worldManager.initWorld(serverLevel);
        });
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
                return;
            }
            this.worldManager.worldUnloaded(serverLevel);
        });
        MinecraftForge.EVENT_BUS.addListener(new TickEndListener());
        MinecraftForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> this.common.shutdown());
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> this.common.updateCheck());
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

    private final class TickEndListener implements Consumer<TickEvent.ServerTickEvent> {
        private long tick = 0;

        @Override
        public void accept(final TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
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
