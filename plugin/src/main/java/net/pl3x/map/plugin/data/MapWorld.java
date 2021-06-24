package net.pl3x.map.plugin.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.pl3x.map.api.LayerProvider;
import net.pl3x.map.api.Registry;
import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.api.LayerRegistry;
import net.pl3x.map.plugin.api.SpawnIconProvider;
import net.pl3x.map.plugin.api.WorldBorderProvider;
import net.pl3x.map.plugin.configuration.WorldAdvanced;
import net.pl3x.map.plugin.configuration.WorldConfig;
import net.pl3x.map.plugin.task.UpdateMarkers;
import net.pl3x.map.plugin.task.render.AbstractRender;
import net.pl3x.map.plugin.task.render.BackgroundRender;
import net.pl3x.map.plugin.task.render.FullRender;
import net.pl3x.map.plugin.util.Colors;
import net.pl3x.map.plugin.visibilitylimit.VisibilityLimit;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class MapWorld implements net.pl3x.map.api.MapWorld {
    private static final String dirtyChunksFileName = "dirty_chunks.json";
    private static final String renderProgressFileName = "resume_render.json";
    private static final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
    private static final Map<UUID, LayerRegistry> layerRegistries = new HashMap<>();

    private final ServerLevel world;
    private final org.bukkit.World bukkitWorld;
    private final Path dataPath;
    private final ExecutorService imageIOexecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Set<ChunkCoordinate> modifiedChunks = ConcurrentHashMap.newKeySet();
    private final UpdateMarkers updateMarkersTask;
    private final BlockColors blockColors;
    private final VisibilityLimit visibilityLimit;

    private AbstractRender activeRender = null;
    private ScheduledFuture<?> backgroundRender = null;
    private boolean pauseRenders = false;

    private MapWorld(final org.bukkit.@NonNull World world) {
        this.bukkitWorld = world;
        this.world = ((CraftWorld) world).getHandle();

        this.blockColors = new BlockColors(this);

        this.dataPath = Pl3xMapPlugin.getInstance().getDataFolder().toPath().resolve("data").resolve(world.getName());
        try {
            if (!Files.exists(this.dataPath)) {
                Files.createDirectories(this.dataPath);
            }
        } catch (IOException e) {
            throw this.failedToCreateDataDirectory(e);
        }
        this.startBackgroundRender();

        this.updateMarkersTask = new UpdateMarkers(this);
        this.updateMarkersTask.runTaskTimer(Pl3xMapPlugin.getInstance(), 20 * 5, 20L * this.config().MARKER_API_UPDATE_INTERVAL_SECONDS);

        this.layerRegistry(); // init the layer registry
        if (this.config().SPAWN_MARKER_ICON_ENABLED) {
            this.layerRegistry().register(SpawnIconProvider.SPAWN_ICON_KEY, new SpawnIconProvider(this));
        }
        if (this.config().WORLDBORDER_MARKER_ENABLED) {
            this.layerRegistry().register(WorldBorderProvider.WORLDBORDER_KEY, new WorldBorderProvider(this));
        }

        this.visibilityLimit = new VisibilityLimit(world);
        this.visibilityLimit.parse(this.config().VISIBILITY_LIMITS);

        this.deserializeDirtyChunks();

        if (getRenderProgress() != null) {
            startRender(new FullRender(this));
        }
    }

    public Map<Region, Boolean> getRenderProgress() {
        try {
            final Path file = this.dataPath.resolve(renderProgressFileName);
            if (Files.exists(file)) {
                String json = String.join("", Files.readAllLines(file));
                TypeToken<LinkedHashMap<Region, Boolean>> token = new TypeToken<>() {
                };
                return gson.fromJson(json, token.getType());
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            Logger.warn(String.format("Failed to deserialize render progress for world '%s'", this.name()), e);
        }
        return null;
    }

    public void saveRenderProgress(Map<Region, Boolean> regions) {
        try {
            Files.writeString(this.dataPath.resolve(renderProgressFileName), gson.toJson(regions));
        } catch (IOException e) {
            Logger.warn(String.format("Failed to serialize render progress for world '%s'", this.name()), e);
        }
    }

    private void serializeDirtyChunks() {
        try {
            Files.writeString(this.dataPath.resolve(dirtyChunksFileName), gson.toJson(this.modifiedChunks));
        } catch (IOException e) {
            Logger.warn(String.format("Failed to serialize dirty chunks for world '%s'", this.name()), e);
        }
    }

    private void deserializeDirtyChunks() {
        try {
            final Path file = this.dataPath.resolve(dirtyChunksFileName);
            if (Files.exists(file)) {
                this.modifiedChunks.addAll(gson.fromJson(
                        new FileReader(file.toFile()),
                        TypeToken.getParameterized(List.class, ChunkCoordinate.class).getType()
                ));
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            Logger.warn(String.format("Failed to deserialize dirty chunks for world '%s'", this.name()), e);
        }
    }

    private void startBackgroundRender() {
        if (this.backgroundRendering() || this.isRendering()) {
            throw new IllegalStateException("Already rendering");
        }
        if (!this.config().BACKGROUND_RENDER_ENABLED) {
            return;
        }
        final BackgroundRender render = new BackgroundRender(this);
        this.backgroundRender = this.executor.scheduleAtFixedRate(render, this.config().BACKGROUND_RENDER_INTERVAL_SECONDS, this.config().BACKGROUND_RENDER_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void stopBackgroundRender() {
        if (!this.backgroundRendering()) {
            throw new IllegalStateException("Not background rendering");
        }
        this.backgroundRender.cancel(false);
        this.backgroundRender = null;
    }

    private boolean backgroundRendering() {
        return this.backgroundRender != null;
    }

    public void chunkModified(final @NonNull ChunkCoordinate coord) {
        if (this.visibilityLimit().shouldRenderChunk(coord)) {
            this.modifiedChunks.add(coord);
        }
    }

    public boolean hasModifiedChunks() {
        return !this.modifiedChunks.isEmpty();
    }

    public @NonNull ChunkCoordinate nextModifiedChunk() {
        final Iterator<ChunkCoordinate> it = this.modifiedChunks.iterator();
        final ChunkCoordinate coord = it.next();
        it.remove();
        return coord;
    }

    public static @NonNull MapWorld forWorld(final org.bukkit.@NonNull World world) {
        return new MapWorld(world);
    }

    @Override
    public @NonNull String name() {
        return this.bukkitWorld.getName();
    }

    @Override
    public @NonNull UUID uuid() {
        return this.bukkitWorld.getUID();
    }

    public @NonNull WorldConfig config() {
        return WorldConfig.get(this.bukkitWorld);
    }

    public @NonNull WorldAdvanced advanced() {
        return WorldAdvanced.get(this.bukkitWorld);
    }

    public ServerLevel nms() {
        return world;
    }

    public org.bukkit.@NonNull World bukkit() {
        return this.bukkitWorld;
    }

    public int getMapColor(final @NonNull BlockState state) {
        final int special = blockColors.getColor(state);
        if (special != -1) {
            return special;
        }
        return Colors.rgb(state.getMapColor(null, null));
    }

    public boolean isRendering() {
        return this.activeRender != null;
    }

    public boolean rendersPaused() {
        return pauseRenders;
    }

    public void pauseRenders(boolean pauseRenders) {
        this.pauseRenders = pauseRenders;
    }

    public void finishedRender() {
        try {
            Files.deleteIfExists(this.dataPath.resolve(renderProgressFileName));
        } catch (IOException e) {
            Logger.warn(String.format("Failed to delete render progress data for world '%s'", this.name()), e);
        }
    }

    public void stopRender() {
        if (!this.isRendering()) {
            throw new IllegalStateException("No render to stop");
        }
        this.activeRender.cancel();
        this.activeRender = null;
        this.startBackgroundRender();
    }

    public void startRender(final @NonNull AbstractRender render) {
        if (this.isRendering()) {
            throw new IllegalStateException("Already rendering");
        }
        if (this.backgroundRendering()) {
            this.stopBackgroundRender();
        }
        this.activeRender = render;
        executor.submit(this.activeRender.getFutureTask());
    }

    public void shutdown() {
        if (this.layerRegistry().hasEntry(SpawnIconProvider.SPAWN_ICON_KEY)) {
            this.layerRegistry().unregister(SpawnIconProvider.SPAWN_ICON_KEY);
        }
        if (this.layerRegistry().hasEntry(WorldBorderProvider.WORLDBORDER_KEY)) {
            this.layerRegistry().unregister(WorldBorderProvider.WORLDBORDER_KEY);
        }
        this.updateMarkersTask.cancel();
        if (this.isRendering()) {
            this.stopRender();
        }
        if (this.backgroundRendering()) {
            this.stopBackgroundRender();
        }
        executor.shutdown();
        imageIOexecutor.shutdown();
        this.serializeDirtyChunks();
    }

    public void saveImage(final @NonNull Image image) {
        this.imageIOexecutor.submit(image::save);
    }

    @Override
    public @NonNull Registry<LayerProvider> layerRegistry() {
        final LayerRegistry registry = layerRegistries.get(this.uuid());
        if (registry == null) {
            final LayerRegistry newRegistry = new LayerRegistry();
            layerRegistries.put(this.uuid(), newRegistry);
            return newRegistry;
        }
        return registry;
    }

    private @NonNull IllegalStateException failedToCreateDataDirectory(final @NonNull Throwable cause) {
        return new IllegalStateException(String.format("Failed to create data directory for world '%s'", this.name()), cause);
    }

    @Override
    public @NonNull VisibilityLimit visibilityLimit() {
        return visibilityLimit;
    }

}
