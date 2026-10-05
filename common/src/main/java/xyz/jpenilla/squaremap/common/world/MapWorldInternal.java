package xyz.jpenilla.squaremap.common.world;

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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.SquaremapDirectories;
import xyz.jpenilla.squaremap.common.api.LayerRegistry;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.config.WorldAdvanced;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.coordinate.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.render.RenderFactory;
import xyz.jpenilla.squaremap.common.render.RenderScheduler;
import xyz.jpenilla.squaremap.common.render.output.RegionImage;
import xyz.jpenilla.squaremap.common.render.output.RegionImageSaveQueue;
import xyz.jpenilla.squaremap.common.render.scanning.BiomeColorTables;
import xyz.jpenilla.squaremap.common.render.scanning.BlockColors;
import xyz.jpenilla.squaremap.common.util.FileUtil;
import xyz.jpenilla.squaremap.common.util.Json;
import xyz.jpenilla.squaremap.common.visibilitylimit.VisibilityLimitImpl;
import xyz.jpenilla.squaremap.common.world.layer.SpawnIconLayer;
import xyz.jpenilla.squaremap.common.world.layer.WorldBorderLayer;

@DefaultQualifier(NonNull.class)
public abstract class MapWorldInternal implements MapWorld {
    private static final String DIRTY_CHUNKS_FILE_NAME = "dirty_chunks.json";
    private static final Map<WorldIdentifier, LayerRegistry> LAYER_REGISTRIES = new HashMap<>();

    private final ServerLevel level;
    private final WorldConfig worldConfig;
    private final WorldAdvanced advancedWorldConfig;
    private final Path dataPath;
    private final Path tilesPath;
    private final RegionImageSaveQueue regionImageSaveQueue;
    private final SnapshotRequests snapshotRequests = new SnapshotRequests(1);
    private final RenderScheduler renderScheduler;
    private final Set<ChunkCoordinate> modifiedChunks = ConcurrentHashMap.newKeySet();
    private final BlockColors blockColors;
    private final BiomeColorTables biomeColorTables;
    private final VisibilityLimitImpl visibilityLimit;
    private volatile long lastReset = -1;

    protected MapWorldInternal(
        final ServerLevel level,
        final RenderFactory renderFactory,
        final SquaremapDirectories directories,
        final ConfigManager configManager
    ) {
        this.level = level;

        this.worldConfig = configManager.worldConfig(level);
        this.advancedWorldConfig = configManager.worldAdvanced(level);
        this.regionImageSaveQueue = RegionImageSaveQueue.create(level, this.worldConfig.IMAGE_SAVING);

        this.blockColors = new BlockColors(this.advancedWorldConfig);
        this.biomeColorTables = BiomeColorTables.create(level, this.advancedWorldConfig);

        this.dataPath = directories.getAndCreateDataDirectory(level);
        this.tilesPath = directories.getAndCreateTilesDirectory(level);

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

        this.renderScheduler = RenderScheduler.create(this, renderFactory);
    }

    @Override
    public Registry<LayerProvider> layerRegistry() {
        return LAYER_REGISTRIES.computeIfAbsent(this.identifier(), $ -> new LayerRegistry());
    }

    @Override
    public WorldIdentifier identifier() {
        return WorldIdentifiers.identifier(this.level);
    }

    public RenderScheduler renderScheduler() {
        return this.renderScheduler;
    }

    public SnapshotRequests snapshotRequests() {
        return this.snapshotRequests;
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

    public BiomeColorTables biomeColorTables() {
        return this.biomeColorTables;
    }

    public BlockColors blockColors() {
        return this.blockColors;
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

    public @Nullable BlockPos getSpawnPos() {
        final LevelData.RespawnData respawnData = this.level.getServer().getRespawnData();
        if (respawnData.dimension().equals(this.level.dimension())) {
            return respawnData.pos();
        }
        return null;
    }

    public final BlockPos getAnySpawnPos() {
        @Nullable BlockPos pos = this.getSpawnPos();
        if (pos == null) {
            pos = this.level.getServer().getRespawnData().pos();
        }
        return pos;
    }

    public void saveImage(final RegionImage image) throws InterruptedException {
        this.regionImageSaveQueue.saveImage(image);
    }

    public void chunkModified(final ChunkCoordinate coord) {
        if (!this.config().BACKGROUND_RENDER.enabled) {
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
        this.renderScheduler.shutdown();
        this.regionImageSaveQueue.shutdown();
        this.serializeDirtyChunks();
    }

    private void serializeDirtyChunks() {
        final Path file = this.dataPath.resolve(DIRTY_CHUNKS_FILE_NAME);
        if (this.modifiedChunks.size() > 200000) { // ~6MB
            Logging.logger().warn("Map for world '{}' has a large amount ({}) of chunks queued for background render! If this notice appears frequently, consider adjusting the background render and or update trigger settings.", this.identifier().asString(), this.modifiedChunks.size());
        }
        try {
            FileUtil.atomicWrite(file, tmp -> Files.writeString(tmp, Json.gson().toJson(this.modifiedChunks)));
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
            deserialized = Json.gson().fromJson(reader, new TypeToken<List<ChunkCoordinate>>() {}.getType());
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
