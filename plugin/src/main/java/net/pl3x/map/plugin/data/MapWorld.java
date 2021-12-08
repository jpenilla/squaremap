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
import net.pl3x.map.plugin.Logging;
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
import net.pl3x.map.plugin.util.RecordTypeAdapterFactory;
import net.pl3x.map.plugin.util.ReflectionUtil;
import net.pl3x.map.plugin.util.Util;
import net.pl3x.map.plugin.visibilitylimit.VisibilityLimit;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class MapWorld implements net.pl3x.map.api.MapWorld {
    private static final String DIRTY_CHUNKS_FILE_NAME = "dirty_chunks.json";
    private static final String RENDER_PROGRESS_FILE_NAME = "resume_render.json";
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapterFactory(new RecordTypeAdapterFactory())
        .enableComplexMapKeySerialization()
        .create();
    private static final Map<UUID, LayerRegistry> LAYER_REGISTRIES = new HashMap<>();

    private final ServerLevel level;
    private final org.bukkit.World world;
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
        this.world = world;
        this.level = ReflectionUtil.CraftBukkit.serverLevel(world);

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

        if (this.getRenderProgress() != null) {
            this.startRender(new FullRender(this));
        }
    }

    public Map<RegionCoordinate, Boolean> getRenderProgress() {
        try {
            final Path file = this.dataPath.resolve(RENDER_PROGRESS_FILE_NAME);
            if (Files.exists(file)) {
                String json = String.join("", Files.readAllLines(file));
                TypeToken<LinkedHashMap<RegionCoordinate, Boolean>> token = new TypeToken<>() {
                };
                return GSON.fromJson(json, token.getType());
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            Logging.warn(String.format("Failed to deserialize render progress for world '%s'", this.name()), e);
        }
        return null;
    }

    public void saveRenderProgress(Map<RegionCoordinate, Boolean> regions) {
        try {
            Files.writeString(this.dataPath.resolve(RENDER_PROGRESS_FILE_NAME), GSON.toJson(regions));
        } catch (IOException e) {
            Logging.warn(String.format("Failed to serialize render progress for world '%s'", this.name()), e);
        }
    }

    private void serializeDirtyChunks() {
        try {
            Files.writeString(this.dataPath.resolve(DIRTY_CHUNKS_FILE_NAME), GSON.toJson(this.modifiedChunks));
        } catch (IOException e) {
            Logging.warn(String.format("Failed to serialize dirty chunks for world '%s'", this.name()), e);
        }
    }

    private void deserializeDirtyChunks() {
        try {
            final Path file = this.dataPath.resolve(DIRTY_CHUNKS_FILE_NAME);
            if (Files.exists(file)) {
                this.modifiedChunks.addAll(
                    GSON.fromJson(
                        new FileReader(file.toFile()),
                        TypeToken.getParameterized(List.class, ChunkCoordinate.class).getType()
                    )
                );
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            Logging.warn(String.format("Failed to deserialize dirty chunks for world '%s'", this.name()), e);
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
        return this.world.getName();
    }

    @Override
    public @NonNull UUID uuid() {
        return this.world.getUID();
    }

    public @NonNull WorldConfig config() {
        return WorldConfig.get(this.world);
    }

    public @NonNull WorldAdvanced advanced() {
        return WorldAdvanced.get(this.world);
    }

    public ServerLevel serverLevel() {
        return this.level;
    }

    public org.bukkit.@NonNull World bukkit() {
        return this.world;
    }

    public int getMapColor(final @NonNull BlockState state) {
        final int special = this.blockColors.getColor(state);
        if (special != -1) {
            return special;
        }
        return Colors.rgb(state.getMapColor(null, null));
    }

    public boolean isRendering() {
        return this.activeRender != null;
    }

    public boolean rendersPaused() {
        return this.pauseRenders;
    }

    public void pauseRenders(boolean pauseRenders) {
        this.pauseRenders = pauseRenders;
    }

    public void finishedRender() {
        try {
            Files.deleteIfExists(this.dataPath.resolve(RENDER_PROGRESS_FILE_NAME));
        } catch (IOException e) {
            Logging.warn(String.format("Failed to delete render progress data for world '%s'", this.name()), e);
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
        this.executor.submit(this.activeRender.getFutureTask());
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
        Util.shutdownExecutor(this.executor, TimeUnit.SECONDS, 1L);
        Util.shutdownExecutor(this.imageIOexecutor, TimeUnit.SECONDS, 2L);
        this.serializeDirtyChunks();
    }

    public void saveImage(final @NonNull Image image) {
        this.imageIOexecutor.submit(image::save);
    }

    @Override
    public @NonNull Registry<LayerProvider> layerRegistry() {
        final LayerRegistry registry = LAYER_REGISTRIES.get(this.uuid());
        if (registry == null) {
            final LayerRegistry newRegistry = new LayerRegistry();
            LAYER_REGISTRIES.put(this.uuid(), newRegistry);
            return newRegistry;
        }
        return registry;
    }

    private @NonNull IllegalStateException failedToCreateDataDirectory(final @NonNull Throwable cause) {
        return new IllegalStateException(String.format("Failed to create data directory for world '%s'", this.name()), cause);
    }

    @Override
    public @NonNull VisibilityLimit visibilityLimit() {
        return this.visibilityLimit;
    }

}
