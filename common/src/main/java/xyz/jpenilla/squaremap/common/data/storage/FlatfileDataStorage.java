package xyz.jpenilla.squaremap.common.data.storage;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.RecordTypeAdapterFactory;

@DefaultQualifier(NonNull.class)
public class FlatfileDataStorage implements DataStorage {
    private static final String DIRTY_CHUNKS_FILE_NAME = "dirty_chunks.json";
    private static final String RENDER_PROGRESS_FILE_NAME = "resume_render.json";
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapterFactory(new RecordTypeAdapterFactory())
        .enableComplexMapKeySerialization()
        .create();

    private final Map<String, Path> dataPaths = new ConcurrentHashMap<>();

    private Path getDataPath(WorldIdentifier identifier, String worldLevelWebName) {
        return this.dataPaths.computeIfAbsent(worldLevelWebName, k -> {
            Path path = SquaremapCommon.instance().platform().dataDirectory().resolve("data").resolve(
                worldLevelWebName
            );
            try {
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }
            } catch (IOException e) {
                throw new IllegalStateException(String.format("Failed to create data directory for world '%s'", identifier), e);
            }
            return path;
        });
    }

    @Override
    public void storeDirtyChunks(WorldIdentifier world, Set<ChunkCoordinate> dirtyChunkCoordinates, AdditionalParameters parameters) {
        Path dataPath = getDataPath(
            world,
            (String) parameters.get("levelWebName").orElseThrow(() -> new IllegalArgumentException("Couldn't get levelWebName"))
        );
        try {
            Files.writeString(dataPath.resolve(DIRTY_CHUNKS_FILE_NAME), GSON.toJson(dirtyChunkCoordinates));
        } catch (IOException e) {
            Logging.logger().warn("Failed to serialize dirty chunks for world '{}'", world.asString(), e);
        }
    }

    @Override
    public CompletableFuture<Set<ChunkCoordinate>> getDirtyChunks(WorldIdentifier world, AdditionalParameters parameters) {
        Path dataPath = getDataPath(
            world,
            (String) parameters.get("levelWebName").orElseThrow(() -> new IllegalArgumentException("Couldn't get levelWebName"))
        );
        Set<ChunkCoordinate> ret = ConcurrentHashMap.newKeySet();
        try {
            final Path file = dataPath.resolve(DIRTY_CHUNKS_FILE_NAME);
            if (Files.isRegularFile(file)) {
                try (final BufferedReader reader = Files.newBufferedReader(file)) {
                    ret.addAll(
                        GSON.fromJson(
                            reader,
                            TypeToken.getParameterized(List.class, ChunkCoordinate.class).getType()
                        )
                    );
                }
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            Logging.logger().warn("Failed to deserialize dirty chunks for world '{}'", world.asString(), e);
        }
        return CompletableFuture.completedFuture(ret);
    }

    @Override
    public void storeRenderProgress(WorldIdentifier world, Map<RegionCoordinate, Boolean> renderProgress, AdditionalParameters parameters) {
        Path dataPath = getDataPath(
            world,
            (String) parameters.get("levelWebName").orElseThrow(() -> new IllegalArgumentException("Couldn't get levelWebName"))
        );
        try {
            Files.writeString(dataPath.resolve(RENDER_PROGRESS_FILE_NAME), GSON.toJson(renderProgress));
        } catch (IOException e) {
            Logging.logger().warn("Failed to serialize render progress for world '{}'", world.asString(), e);
        }
    }

    @Override
    public CompletableFuture<Map<RegionCoordinate, Boolean>> getRenderProgress(WorldIdentifier world, AdditionalParameters parameters) {
        Path dataPath = getDataPath(
            world,
            (String) parameters.get("levelWebName").orElseThrow(() -> new IllegalArgumentException("Couldn't get levelWebName"))
        );
        try {
            final Path file = dataPath.resolve(RENDER_PROGRESS_FILE_NAME);
            if (Files.isRegularFile(file)) {
                final Type type = new TypeToken<LinkedHashMap<RegionCoordinate, Boolean>>() {
                }.getType();
                try (final BufferedReader reader = Files.newBufferedReader(file)) {
                    return CompletableFuture.completedFuture(GSON.fromJson(reader, type));
                }
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            Logging.logger().warn("Failed to deserialize render progress for world '{}'", world.asString(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void deleteRenderProgress(WorldIdentifier world, AdditionalParameters parameters) {
        Path dataPath = getDataPath(
            world,
            (String) parameters.get("levelWebName").orElseThrow(() -> new IllegalArgumentException("Couldn't get levelWebName"))
        );
        try {
            Files.deleteIfExists(dataPath.resolve(RENDER_PROGRESS_FILE_NAME));
        } catch (IOException e) {
            Logging.logger().warn("Failed to delete render progress data for world '{}'", world.asString(), e);
        }
    }

    @Override
    public void updateMarkers(WorldIdentifier world, List<Map<String, Object>> layers, AdditionalParameters parameters) {
        // todo
    }

    @Override
    public void updateWorldSettings(WorldIdentifier world, Map<String, Object> settings, AdditionalParameters parameters) {
        // todo
    }

    @Override
    public void updateGlobalSettings(WorldIdentifier world, Map<String, Object> settings, AdditionalParameters parameters) {
        // todo
    }
}
