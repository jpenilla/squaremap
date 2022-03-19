package xyz.jpenilla.squaremap.common.data;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.LayerRegistry;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.config.WorldAdvanced;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.layer.SpawnIconProvider;
import xyz.jpenilla.squaremap.common.layer.WorldBorderProvider;
import xyz.jpenilla.squaremap.common.task.render.AbstractRender;
import xyz.jpenilla.squaremap.common.task.render.BackgroundRender;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;
import xyz.jpenilla.squaremap.common.util.Colors;
import xyz.jpenilla.squaremap.common.util.RecordTypeAdapterFactory;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.common.visibilitylimit.VisibilityLimitImpl;

@DefaultQualifier(NonNull.class)
public abstract class MapWorldInternal implements MapWorld {
    private static final String DIRTY_CHUNKS_FILE_NAME = "dirty_chunks.json";
    private static final String RENDER_PROGRESS_FILE_NAME = "resume_render.json";
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapterFactory(new RecordTypeAdapterFactory())
        .enableComplexMapKeySerialization()
        .create();
    private static final Map<WorldIdentifier, LayerRegistry> LAYER_REGISTRIES = new HashMap<>();

    private final SquaremapPlatform platform;
    private final ServerLevel level;
    private final RenderFactory renderFactory;
    private final Path dataPath;
    private final Path tilesPath;
    private final ExecutorService imageIOexecutor;
    private final ScheduledExecutorService executor;
    private final Set<ChunkCoordinate> modifiedChunks = ConcurrentHashMap.newKeySet();
    private final BlockColors blockColors;
    private final LevelBiomeColorData levelBiomeColorData;
    private final VisibilityLimitImpl visibilityLimit;

    private volatile boolean pauseRenders = false;
    private WorldConfig worldConfig;
    private WorldAdvanced advancedWorldConfig;
    private @Nullable AbstractRender activeRender = null;
    private @Nullable ScheduledFuture<?> backgroundRender = null;

    protected MapWorldInternal(
        final SquaremapPlatform platform,
        final ServerLevel level,
        final RenderFactory renderFactory,
        final DirectoryProvider directoryProvider
    ) {
        this.platform = platform;
        this.level = level;
        this.renderFactory = renderFactory;

        this.imageIOexecutor = Executors.newSingleThreadExecutor(
            Util.squaremapThreadFactory("imageio", this.level)
        );
        this.executor = Executors.newSingleThreadScheduledExecutor(
            Util.squaremapThreadFactory("render", this.level)
        );

        // Keep updated references to world configs to avoid constant HashMap lookups during renders
        this.worldConfig = WorldConfig.get(this.level);
        this.advancedWorldConfig = WorldAdvanced.get(this.level);

        this.blockColors = new BlockColors(this);
        this.levelBiomeColorData = LevelBiomeColorData.create(this);

        this.dataPath = this.getAndCreateDataDirectory(directoryProvider);
        this.tilesPath = directoryProvider.getAndCreateTilesDirectory(this.serverLevel());

        this.startBackgroundRender();

        this.layerRegistry(); // init the layer registry
        if (this.config().SPAWN_MARKER_ICON_ENABLED) {
            this.layerRegistry().register(SpawnIconProvider.SPAWN_ICON_KEY, new SpawnIconProvider(this));
        }
        if (this.config().WORLDBORDER_MARKER_ENABLED) {
            this.layerRegistry().register(WorldBorderProvider.WORLDBORDER_KEY, new WorldBorderProvider(this));
        }

        this.visibilityLimit = new VisibilityLimitImpl(this);
        this.visibilityLimit.parse(this.config().VISIBILITY_LIMITS);

        this.deserializeDirtyChunks();

        if (this.getRenderProgress() != null) {
            this.startRender(this.renderFactory.createFullRender(this, 2));
        }
    }

    public @Nullable Map<RegionCoordinate, Boolean> getRenderProgress() {
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
            Logging.logger().warn("Failed to deserialize render progress for world '{}'", this.identifier().asString(), e);
        }
        return null;
    }

    public void saveRenderProgress(Map<RegionCoordinate, Boolean> regions) {
        try {
            Files.writeString(this.dataPath.resolve(RENDER_PROGRESS_FILE_NAME), GSON.toJson(regions));
        } catch (IOException e) {
            Logging.logger().warn("Failed to serialize render progress for world '{}'", this.identifier().asString(), e);
        }
    }

    private void serializeDirtyChunks() {
        try {
            Files.writeString(this.dataPath.resolve(DIRTY_CHUNKS_FILE_NAME), GSON.toJson(this.modifiedChunks));
        } catch (IOException e) {
            Logging.logger().warn("Failed to serialize dirty chunks for world '{}'", this.identifier().asString(), e);
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
            Logging.logger().warn("Failed to deserialize dirty chunks for world '{}'", this.identifier().asString(), e);
        }
    }

    private void startBackgroundRender() {
        if (this.backgroundRendering() || this.isRendering()) {
            throw new IllegalStateException("Already rendering");
        }
        if (!this.config().BACKGROUND_RENDER_ENABLED) {
            return;
        }
        final BackgroundRender render = this.renderFactory.createBackgroundRender(this);
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

    public void chunkModified(final ChunkCoordinate coord) {
        if (!this.config().BACKGROUND_RENDER_ENABLED) {
            return;
        }
        if (!this.visibilityLimit().shouldRenderChunk(coord)) {
            return;
        }
        this.modifiedChunks.add(coord);
    }

    public boolean hasModifiedChunks() {
        return !this.modifiedChunks.isEmpty();
    }

    public ChunkCoordinate nextModifiedChunk() {
        final Iterator<ChunkCoordinate> it = this.modifiedChunks.iterator();
        final ChunkCoordinate coord = it.next();
        it.remove();
        return coord;
    }

    public WorldConfig config() {
        return this.worldConfig;
    }

    public WorldAdvanced advanced() {
        return this.advancedWorldConfig;
    }

    public ServerLevel serverLevel() {
        return this.level;
    }

    public Path tilesPath() {
        return this.tilesPath;
    }

    @SuppressWarnings("ConstantConditions") // params for getMapColor are never used, check on mc update
    public int getMapColor(final BlockState state) {
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
            Logging.logger().warn("Failed to delete render progress data for world '{}'", this.identifier().asString(), e);
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

    public void startRender(final AbstractRender render) {
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

    public void saveImage(final Image image) {
        this.imageIOexecutor.submit(image::save);
    }

    @Override
    public Registry<LayerProvider> layerRegistry() {
        return LAYER_REGISTRIES.computeIfAbsent(this.identifier(), $ -> new LayerRegistry());
    }

    @Override
    public WorldIdentifier identifier() {
        return Util.worldIdentifier(this.level);
    }

    public LevelBiomeColorData levelBiomeColorData() {
        return this.levelBiomeColorData;
    }

    private Path getAndCreateDataDirectory(final DirectoryProvider directoryProvider) {
        final Path data = directoryProvider.dataDirectory()
            .resolve("data")
            .resolve(Util.levelWebName(this.level));
        try {
            if (!Files.exists(data)) {
                Files.createDirectories(data);
            }
        } catch (final IOException ex) {
            throw this.failedToCreateDataDirectory(ex);
        }
        return data;
    }

    private IllegalStateException failedToCreateDataDirectory(final Throwable cause) {
        return new IllegalStateException("Failed to create data directory for world '%s'".formatted(this.identifier()), cause);
    }

    /**
     * Get the map visibility limit of the world. Only these regions are drawn,
     * even if more chunks exist on disk.
     *
     * @return The visibility limit.
     */
    //@Override
    public VisibilityLimitImpl visibilityLimit() {
        return this.visibilityLimit;
    }

    public void restartRenderProgressLogging() {
        if (this.activeRender != null) {
            this.activeRender.restartProgressLogger();
        }
    }

    public void refreshConfigInstances() {
        this.worldConfig = WorldConfig.get(this.level);
        this.advancedWorldConfig = WorldAdvanced.get(this.level);
    }

    public interface Factory<W> {
        W create(ServerLevel level);
    }
}
