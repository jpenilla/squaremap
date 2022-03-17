package xyz.jpenilla.squaremap.common.data.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;

@DefaultQualifier(NonNull.class)
public interface DataStorage {

    void storeDirtyChunks(WorldIdentifier world, Set<ChunkCoordinate> dirtyChunkCoordinates, AdditionalParameters parameters);

    CompletableFuture<Set<ChunkCoordinate>> getDirtyChunks(WorldIdentifier world, AdditionalParameters parameters);

    void storeRenderProgress(WorldIdentifier world, Map<RegionCoordinate, Boolean> renderProgress, AdditionalParameters parameters);

    CompletableFuture<Map<RegionCoordinate, Boolean>> getRenderProgress(WorldIdentifier world, AdditionalParameters parameters);

    void deleteRenderProgress(WorldIdentifier world, AdditionalParameters parameters);

    void updateMarkers(WorldIdentifier world, List<Map<String, Object>> layers, AdditionalParameters parameters);

    void updateWorldSettings(WorldIdentifier world, Map<String, Object> settings, AdditionalParameters parameters);

    void updateGlobalSettings(WorldIdentifier world, Map<String, Object> settings, AdditionalParameters parameters);
}
