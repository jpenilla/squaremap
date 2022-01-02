package xyz.jpenilla.squaremap.plugin;

import cloud.commandframework.CommandManager;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import io.leangen.geantyref.TypeToken;
import io.papermc.paper.text.PaperComponents;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.logging.log4j.Logger;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.argument.LevelArgument;
import xyz.jpenilla.squaremap.common.command.argument.MapWorldArgument;
import xyz.jpenilla.squaremap.common.util.BiomeSpecialEffectsAccess;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.plugin.command.PaperCommander;
import xyz.jpenilla.squaremap.plugin.command.PaperCommands;
import xyz.jpenilla.squaremap.plugin.listener.MapUpdateListeners;
import xyz.jpenilla.squaremap.plugin.listener.PlayerListener;
import xyz.jpenilla.squaremap.plugin.listener.WorldEventListener;
import xyz.jpenilla.squaremap.plugin.network.Network;
import xyz.jpenilla.squaremap.plugin.task.UpdatePlayers;
import xyz.jpenilla.squaremap.plugin.task.UpdateWorldData;
import xyz.jpenilla.squaremap.plugin.util.CraftBukkitReflection;
import xyz.jpenilla.squaremap.plugin.util.PaperChunkSnapshotProvider;

public final class SquaremapPlugin extends JavaPlugin implements SquaremapPlatform {
    private static SquaremapPlugin instance;
    private SquaremapCommon common;
    private PaperWorldManager worldManager;
    private PaperPlayerManager playerManager;
    private UpdateWorldData updateWorldData;
    private UpdatePlayers updatePlayers;
    private MapUpdateListeners mapUpdateListeners;
    private WorldEventListener worldEventListener;

    public SquaremapPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
        } catch (ClassNotFoundException e) {
            this.getLogger().severe("squaremap requires Paper or one of its forks to run. Get Paper from https://papermc.io/downloads");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.common = new SquaremapCommon(this);
        PaperCommands.register(this.common);
        this.getServer().getServicesManager().register(Squaremap.class, this.common.api(), this, ServicePriority.Normal);

        Network.register();

        this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        new Metrics(this, 13571); // https://bstats.org/plugin/bukkit/squaremap/13571
    }

    @Override
    public void onDisable() {
        Network.unregister();

        this.getServer().getServicesManager().unregister(Squaremap.class, this.common.api());
        this.common.shutdown();
    }

    public static SquaremapPlugin getInstance() {
        return instance;
    }

    @Override
    public void startCallback() {
        this.playerManager = new PaperPlayerManager();

        this.updatePlayers = new UpdatePlayers(this);
        this.updatePlayers.runTaskTimer(this, 20, 20);

        this.updateWorldData = new UpdateWorldData();
        this.updateWorldData.runTaskTimer(this, 0, 20 * 5);

        this.worldManager = new PaperWorldManager();
        this.worldManager.start();

        this.mapUpdateListeners = new MapUpdateListeners(this);
        this.mapUpdateListeners.register();

        this.worldEventListener = new WorldEventListener(this);
        this.getServer().getPluginManager().registerEvents(this.worldEventListener, this);
    }

    @Override
    public void stopCallback() {
        if (this.mapUpdateListeners != null) {
            this.mapUpdateListeners.unregister();
            this.mapUpdateListeners = null;
        }

        if (this.worldEventListener != null) {
            HandlerList.unregisterAll(this.worldEventListener);
            this.worldEventListener = null;
        }

        if (this.updatePlayers != null) {
            if (!this.updatePlayers.isCancelled()) {
                this.updatePlayers.cancel();
            }
            this.updatePlayers = null;
        }

        if (this.updateWorldData != null) {
            if (!this.updateWorldData.isCancelled()) {
                this.updateWorldData.cancel();
            }
            this.updateWorldData = null;
        }

        if (this.worldManager != null) {
            this.worldManager.shutdown();
            this.worldManager = null;
        }

        if (this.playerManager != null) {
            this.playerManager = null;
        }

        this.getServer().getScheduler().cancelTasks(this);
    }

    @Override
    public @NonNull CommandManager<Commander> createCommandManager() {
        final PaperCommandManager<Commander> mgr;
        try {
            mgr = new PaperCommandManager<>(
                this,
                CommandExecutionCoordinator.simpleCoordinator(),
                sender -> {
                    if (sender instanceof Player player) {
                        return new PaperCommander.Player(player);
                    }
                    return new PaperCommander(sender);
                },
                commander -> ((PaperCommander) commander).sender()
            );
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to initialize command manager", ex);
        }

        if (mgr.queryCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            mgr.registerBrigadier();
            final @Nullable CloudBrigadierManager<Commander, ?> brigManager = mgr.brigadierManager();
            if (brigManager != null) {
                brigManager.setNativeNumberSuggestions(false);
                brigManager.registerMapping(
                    new TypeToken<MapWorldArgument.Parser<Commander>>() {
                    },
                    builder -> builder.toConstant(DimensionArgument.dimension()).cloudSuggestions()
                );
                brigManager.registerMapping(
                    new TypeToken<LevelArgument.Parser<Commander>>() {
                    },
                    builder -> builder.toConstant(DimensionArgument.dimension()).cloudSuggestions()
                );
            }
        }

        if (mgr.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            mgr.registerAsynchronousCompletions();
        }

        return mgr;
    }

    @Override
    public @NonNull PaperWorldManager worldManager() {
        return this.worldManager;
    }

    @Override
    public @NonNull PaperPlayerManager playerManager() {
        return this.playerManager;
    }

    public SquaremapCommon common() {
        return this.common;
    }

    @Override
    public @NonNull ChunkSnapshotProvider chunkSnapshotProvider() {
        return PaperChunkSnapshotProvider.get();
    }

    @Override
    public @NonNull Path dataDirectory() {
        return this.getDataFolder().toPath();
    }

    @Override
    public @NonNull Logger logger() {
        return this.getLog4JLogger();
    }

    @Override
    public @NonNull ComponentFlattener componentFlattener() {
        return PaperComponents.flattener();
    }

    @Override
    public @NonNull String configNameForWorld(final @NonNull ServerLevel level) {
        return level.getWorld().getName();
    }

    @Override
    public @NonNull String tilesDirNameForWorld(final @NonNull ServerLevel level) {
        return level.getWorld().getName();
    }

    @Override
    public @NonNull Collection<ServerLevel> levels() {
        final List<ServerLevel> levels = new ArrayList<>();
        for (final World world : Bukkit.getWorlds()) {
            levels.add(((CraftWorld) world).getHandle());
        }
        return levels;
    }

    @Override
    public @Nullable ServerLevel level(final @NonNull WorldIdentifier identifier) {
        final @Nullable World world = Bukkit.getWorld(BukkitAdapter.namespacedKey(identifier));
        if (world == null) {
            return null;
        }
        return ((CraftWorld) world).getHandle();
    }

    @Override
    public @NonNull Path regionFileDirectory(final @NonNull ServerLevel level) {
        return LevelStorageSource.getStorageFolder(level.getWorld().getWorldFolder().toPath(), level.getTypeKey()).resolve("region");
    }

    @Override
    public @NonNull BiomeSpecialEffectsAccess biomeSpecialEffectsAccess() {
        return CraftBukkitReflection.BiomeSpecialEffectsHelper.get();
    }
}
