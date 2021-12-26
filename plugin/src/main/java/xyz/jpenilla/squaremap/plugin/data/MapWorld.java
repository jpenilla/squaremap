package xyz.jpenilla.squaremap.plugin.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
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
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.plugin.Logging;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.api.LayerRegistry;
import xyz.jpenilla.squaremap.plugin.api.SpawnIconProvider;
import xyz.jpenilla.squaremap.plugin.api.WorldBorderProvider;
import xyz.jpenilla.squaremap.plugin.config.WorldAdvanced;
import xyz.jpenilla.squaremap.plugin.config.WorldConfig;
import xyz.jpenilla.squaremap.plugin.task.UpdateMarkers;
import xyz.jpenilla.squaremap.plugin.task.render.AbstractRender;
import xyz.jpenilla.squaremap.plugin.task.render.BackgroundRender;
import xyz.jpenilla.squaremap.plugin.task.render.FullRender;
import xyz.jpenilla.squaremap.plugin.util.Colors;
import xyz.jpenilla.squaremap.plugin.util.RecordTypeAdapterFactory;
import xyz.jpenilla.squaremap.plugin.util.ReflectionUtil;
import xyz.jpenilla.squaremap.plugin.util.Util;
import xyz.jpenilla.squaremap.plugin.visibilitylimit.VisibilityLimit;

public final class MapWorld implements xyz.jpenilla.squaremap.api.MapWorld {
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
    private final ExecutorService imageIOexecutor;
    private final ScheduledExecutorService executor;
    private final Set<ChunkCoordinate> modifiedChunks = ConcurrentHashMap.newKeySet();
    private final UpdateMarkers updateMarkersTask;
    private final BlockColors blockColors;
    private final VisibilityLimit visibilityLimit;

    private volatile boolean pauseRenders = false;
    private WorldConfig worldConfig;
    private WorldAdvanced advancedWorldConfig;
    private AbstractRender activeRender = null;
    private ScheduledFuture<?> backgroundRender = null;

    private MapWorld(final org.bukkit.@NonNull World world) {
        this.world = world;
        this.level = ReflectionUtil.CraftBukkit.serverLevel(world);

        this.imageIOexecutor = Executors.newSingleThreadExecutor(
            Util.squaremapThreadFactory("imageio", this.level)
        );
        this.executor = Executors.newSingleThreadScheduledExecutor(
            Util.squaremapThreadFactory("render", this.level)
        );

        // Keep updated references to world configs to avoid constant HashMap lookups during renders
        this.worldConfig = WorldConfig.get(world);
        this.advancedWorldConfig = WorldAdvanced.get(world);

        this.blockColors = new BlockColors(this);

        this.dataPath = SquaremapPlugin.getInstance().getDataFolder().toPath().resolve("data").resolve(world.getName());
        try {
            if (!Files.exists(this.dataPath)) {
                Files.createDirectories(this.dataPath);
            }
        } catch (IOException e) {
            throw this.failedToCreateDataDirectory(e);
        }
        this.startBackgroundRender();

        this.updateMarkersTask = new UpdateMarkers(this);
        this.updateMarkersTask.runTaskTimer(SquaremapPlugin.getInstance(), 20 * 5, 20L * this.config().MARKER_API_UPDATE_INTERVAL_SECONDS);

        this.layerRegistry(); // init the layer registry
        if (this.config().SPAWN_MARKER_ICON_ENABLED) {
            this.layerRegistry().register(SpawnIconProvider.SPAWN_ICON_KEY, new SpawnIconProvider(this));
        }
        if (this.config().WORLDBORDER_MARKER_ENABLED) {
            this.layerRegistry().register(WorldBorderProvider.WORLDBORDER_KEY, new WorldBorderProvider(this));
        }

        this.visibilityLimit = new VisibilityLimit(this);
        this.visibilityLimit.parse(this.config().VISIBILITY_LIMITS);

        this.deserializeDirtyChunks();

        if (this.getRenderProgress() != null) {
            this.startRender(new FullRender(this));
        }
    }

    public Map<RegionCoordinate, Boolean> getRenderProgress() {
        try {
            final Path file = this.dataPath.resolve(RENDER_PROGRESS_FILE_NAME);
            if (Files.isRegularFile(file)) {
                final Type type = new TypeToken<LinkedHashMap<RegionCoordinate, Boolean>>() {
                }.getType();
                try (final BufferedReader reader = Files.newBufferedReader(file)) {
                    return GSON.fromJson(reader, type);
                }
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
            if (Files.isRegularFile(file)) {
                try (final BufferedReader reader = Files.newBufferedReader(file)) {
                    this.modifiedChunks.addAll(
                        GSON.fromJson(
                            reader,
                            TypeToken.getParameterized(List.class, ChunkCoordinate.class).getType()
                        )
                    );
                }
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
    public @NonNull WorldIdentifier identifier() {
        return BukkitAdapter.worldIdentifier(this.world);
    }

    public @NonNull UUID uuid() {
        return this.world.getUID();
    }

    public @NonNull WorldConfig config() {
        return this.worldConfig;
    }

    public @NonNull WorldAdvanced advanced() {
        return this.advancedWorldConfig;
    }

    public ServerLevel serverLevel() {
        return this.level;
    }

    public org.bukkit.@NonNull World bukkit() {
        return this.world;
    }

    @SuppressWarnings("ConstantConditions") // params for getMapColor are never used, check on mc update
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

    //@Override
    public @NonNull VisibilityLimit visibilityLimit() {
        return this.visibilityLimit;
    }

    public void restartRenderProgressLogging() {
        if (this.activeRender != null) {
            this.activeRender.restartProgressLogger();
        }
    }

    public void refreshConfigInstances() {
        this.worldConfig = WorldConfig.get(this.world);
        this.advancedWorldConfig = WorldAdvanced.get(this.world);
    }
}
