package xyz.jpenilla.squaremap.fabric;

import cloud.commandframework.CommandManager;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.WorldManagerImpl;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.task.UpdatePlayers;
import xyz.jpenilla.squaremap.common.task.UpdateWorldData;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.common.util.VanillaChunkSnapshotProvider;
import xyz.jpenilla.squaremap.fabric.command.FabricCommands;
import xyz.jpenilla.squaremap.fabric.data.FabricMapWorld;
import xyz.jpenilla.squaremap.fabric.network.FabricNetworking;
import xyz.jpenilla.squaremap.fabric.util.FabricMapUpdates;

import static java.util.Objects.requireNonNull;

@DefaultQualifier(NonNull.class)
public final class SquaremapFabricInitializer implements ModInitializer, SquaremapPlatform {
    private static final Logger LOGGER = LogManager.getLogger("squaremap");

    private @MonotonicNonNull SquaremapCommon common;
    private @Nullable UpdatePlayers updatePlayers;
    private @Nullable UpdateWorldData updateWorldData;
    private @Nullable MinecraftServer minecraftServer;
    private @Nullable WorldManagerImpl<FabricMapWorld> worldManager;
    private @Nullable FabricPlayerManager playerManager;

    @Override
    public void onInitialize() {
        this.common = new SquaremapCommon(this);
        FabricCommands.register(this.common);
        this.registerLifecycleListeners();
        FabricMapUpdates.registerListeners();
        FabricNetworking.register();
        this.common.updateCheck();
    }

    private void registerLifecycleListeners() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> this.minecraftServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (server.isDedicatedServer()) {
                this.common.shutdown();
            }
            this.minecraftServer = null;
        });
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientLifecycleEvents.CLIENT_STOPPING.register($ -> this.common.shutdown());
        }

        ServerWorldEvents.LOAD.register((server, level) -> {
            WorldConfig.get(level);
            this.worldManager().getWorldIfEnabled(level);
        });
        ServerWorldEvents.UNLOAD.register((server, level) -> {
            if (this.worldManager != null) {
                this.worldManager.worldUnloaded(level);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(new TickEndListener());
    }

    @Override
    public void startCallback() {
        this.worldManager = new WorldManagerImpl<>(FabricMapWorld::new);
        this.worldManager.start(this);

        this.playerManager = new FabricPlayerManager();
        this.updatePlayers = new UpdatePlayers(this);
        this.updateWorldData = new UpdateWorldData(this);
    }

    @Override
    public void stopCallback() {
        if (this.worldManager != null) {
            this.worldManager.shutdown();
            this.worldManager = null;
        }

        this.updatePlayers = null;
        this.updateWorldData = null;
        this.playerManager = null;
    }

    public MinecraftServer server() {
        return requireNonNull(this.minecraftServer, "MinecraftServer was requested when not active");
    }

    @Override
    public int maxPlayers() {
        return this.server().getMaxPlayers();
    }

    @Override
    public String version() {
        return FabricLoader.getInstance().getModContainer("squaremap")
            .orElseThrow().getMetadata().getVersion().getFriendlyString();
    }

    @Override
    public ChunkSnapshotProvider chunkSnapshotProvider() {
        return VanillaChunkSnapshotProvider.get();
    }

    @Override
    public WorldManagerImpl<FabricMapWorld> worldManager() {
        return this.worldManager;
    }

    @Override
    public Path dataDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve("squaremap");
    }

    @Override
    public Logger logger() {
        return LOGGER;
    }

    @Override
    public ComponentFlattener componentFlattener() {
        return FabricServerAudiences.of(this.server()).flattener();
    }

    @Override
    public Collection<ServerLevel> levels() {
        if (this.minecraftServer == null) {
            return List.of();
        }
        final List<ServerLevel> levels = new ArrayList<>();
        for (final ServerLevel level : this.server().getAllLevels()) {
            levels.add(level);
        }
        return Collections.unmodifiableList(levels);
    }

    @Override
    public @Nullable ServerLevel level(final WorldIdentifier identifier) {
        for (final ServerLevel level : this.server().getAllLevels()) {
            if (level.dimension().location().getNamespace().equals(identifier.namespace())
                && level.dimension().location().getPath().equals(identifier.value())) {
                return level;
            }
        }
        return null;
    }

    @Override
    public FabricPlayerManager playerManager() {
        return this.playerManager;
    }

    @Override
    public CommandManager<Commander> createCommandManager() {
        return FabricCommands.createCommandManager();
    }

    private final class TickEndListener implements ServerTickEvents.EndTick {
        private long tick = 0;

        @Override
        public void onEndTick(final MinecraftServer server) {
            if (this.tick % 20 == 0) {
                if (this.tick % 100 == 0) {
                    if (SquaremapFabricInitializer.this.updateWorldData != null) {
                        SquaremapFabricInitializer.this.updateWorldData.run();
                    }
                }

                if (SquaremapFabricInitializer.this.updatePlayers != null) {
                    SquaremapFabricInitializer.this.updatePlayers.run();
                }

                if (SquaremapFabricInitializer.this.worldManager != null) {
                    for (final MapWorldInternal mapWorld : SquaremapFabricInitializer.this.worldManager.worlds().values()) {
                        ((FabricMapWorld) mapWorld).tickEachSecond(this.tick);
                    }
                }
            }

            this.tick++;
        }
    }
}
