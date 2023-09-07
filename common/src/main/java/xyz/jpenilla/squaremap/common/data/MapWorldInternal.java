package xyz.jpenilla.squaremap.common.data;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.LayerRegistry;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.config.WorldAdvanced;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.layer.SpawnIconLayer;
import xyz.jpenilla.squaremap.common.layer.WorldBorderLayer;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;
import xyz.jpenilla.squaremap.common.util.Colors;
import xyz.jpenilla.squaremap.common.util.ImageIOExecutor;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.common.visibilitylimit.VisibilityLimitImpl;

@DefaultQualifier(NonNull.class)
public abstract class MapWorldInternal implements MapWorld {
    private static final String DIRTY_CHUNKS_FILE_NAME = "dirty_chunks.json";
    private static final Map<WorldIdentifier, LayerRegistry> LAYER_REGISTRIES = new HashMap<>();

    private final ServerLevel level;
    private final WorldConfig worldConfig;
    private final WorldAdvanced advancedWorldConfig;
    private final Path dataPath;
    private final Path tilesPath;
    private final ImageIOExecutor imageIOExecutor;
    private final RenderManager renderManager;
    private final Set<ChunkCoordinate> modifiedChunks = ConcurrentHashMap.newKeySet();
    private final BlockColors blockColors;
    private final LevelBiomeColorData levelBiomeColorData;
    private final VisibilityLimitImpl visibilityLimit;
    private volatile long lastReset = -1;

    protected MapWorldInternal(
        final ServerLevel level,
        final RenderFactory renderFactory,
        final DirectoryProvider directoryProvider,
        final ConfigManager configManager
    ) {
        this.level = level;

        this.imageIOExecutor = ImageIOExecutor.create(level);

        this.worldConfig = configManager.worldConfig(this.level);
        this.advancedWorldConfig = configManager.worldAdvanced(this.level);

        this.blockColors = BlockColors.create(this);
        this.levelBiomeColorData = LevelBiomeColorData.create(this);

        this.dataPath = directoryProvider.getAndCreateDataDirectory(this.serverLevel());
        this.tilesPath = directoryProvider.getAndCreateTilesDirectory(this.serverLevel());

        this.layerRegistry(); // init the layer registry
        if (this.config().SPAWN_MARKER_ICON_ENABLED) {
            this.layerRegistry().register(SpawnIconLayer.KEY, new SpawnIconLayer(this));
        }
        if (this.config().WORLDBORDER_MARKER_ENABLED) {
            this.layerRegistry().register(WorldBorderLayer.KEY, new WorldBorderLayer(this));
        }

        this.visibilityLimit = new VisibilityLimitImpl(this);
        this.visibilityLimit.load(this.config().VISIBILITY_LIMITS);

        this.deserializeDirtyChunks();

        this.renderManager = RenderManager.create(this, renderFactory);
        this.renderManager.init();
    }

    @Override
    public Registry<LayerProvider> layerRegistry() {
        return LAYER_REGISTRIES.computeIfAbsent(this.identifier(), $ -> new LayerRegistry());
    }

    @Override
    public WorldIdentifier identifier() {
        return Util.worldIdentifier(this.level);
    }

    public RenderManager renderManager() {
        return this.renderManager;
    }

    public Path dataPath() {
        return this.dataPath;
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

    public LevelBiomeColorData levelBiomeColorData() {
        return this.levelBiomeColorData;
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
        final int special = this.blockColors.color(state);
        if (special != -1) {
            return special;
        }
        return Colors.rgb(state.getMapColor(null, null));
    }

    public void saveImage(final Image image) {
        this.imageIOExecutor.saveImage(image);
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

    public void shutdown() {
        if (this.layerRegistry().hasEntry(SpawnIconLayer.KEY)) {
            this.layerRegistry().unregister(SpawnIconLayer.KEY);
        }
        if (this.layerRegistry().hasEntry(WorldBorderLayer.KEY)) {
            this.layerRegistry().unregister(WorldBorderLayer.KEY);
        }
        this.renderManager.shutdown();
        this.imageIOExecutor.shutdown();
        this.serializeDirtyChunks();
    }

    private void serializeDirtyChunks() {
        final Path file = this.dataPath.resolve(DIRTY_CHUNKS_FILE_NAME);
        if (this.modifiedChunks.size() > 200000) { // ~6MB
            Logging.logger().warn("Map for world '{}' has a large amount ({}) of chunks queued for background render! If this notice appears frequently, consider adjusting the background render and or update trigger settings.", this.identifier().asString(), this.modifiedChunks.size());
        }
        try {
            Files.writeString(file, Util.gson().toJson(this.modifiedChunks));
        } catch (final IOException ex) {
            Logging.logger().warn("Failed to serialize dirty chunks for world '{}' to file '{}'", this.identifier().asString(), file, ex);
        }
    }

    private void deserializeDirtyChunks() {
        final Path file = this.dataPath.resolve(DIRTY_CHUNKS_FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return;
        }
        final List<ChunkCoordinate> deserialized;
        try (final BufferedReader reader = Files.newBufferedReader(file)) {
            deserialized = Util.gson().fromJson(reader, new TypeToken<List<ChunkCoordinate>>() {}.getType());
        } catch (final JsonIOException | JsonSyntaxException | IOException ex) {
            Logging.logger().warn("Failed to deserialize dirty chunks for world '{}' from file '{}'", this.identifier().asString(), file, ex);
            return;
        }
        if (deserialized == null) {
            Logging.logger().warn("Failed to deserialize dirty chunks for world '{}' from file '{}' (null result, file is corrupted or empty?)", this.identifier().asString(), file);
            return;
        }
        this.modifiedChunks.addAll(deserialized);
    }

    public void didReset() {
        this.lastReset = System.currentTimeMillis();
    }

    public long lastReset() {
        return this.lastReset;
    }

    public interface Factory {
        MapWorldInternal create(ServerLevel level);
    }
}
