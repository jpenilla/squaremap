package net.pl3x.map.plugin.task.render;

import java.util.ArrayList;
import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.data.ChunkCoordinate;
import net.pl3x.map.plugin.data.Image;
import net.pl3x.map.plugin.data.MapWorld;
import net.pl3x.map.plugin.data.Region;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class BackgroundRender extends AbstractRender {

    public BackgroundRender(final @NonNull MapWorld world) {
        super(world, Executors.newFixedThreadPool(getThreads(world.config().BACKGROUND_RENDER_MAX_THREADS)));
    }

    @Override
    public int totalChunks() {
        return -1; // We don't print progress for background renders
    }

    @Override
    public int totalRegions() {
        return -1; // We don't print progress for background renders
    }

    @Override
    protected void render() {
        long time = System.currentTimeMillis();
        final Set<ChunkCoordinate> chunks = new HashSet<>();
        while (mapWorld.hasModifiedChunks() && chunks.size() < mapWorld.config().BACKGROUND_RENDER_MAX_CHUNKS_PER_INTERVAL) {
            chunks.add(mapWorld.nextModifiedChunk());
        }
        final Map<Region, List<ChunkCoordinate>> coordMap = chunks.stream().collect(Collectors.groupingBy(ChunkCoordinate::regionCoordinate));

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        coordMap.forEach((region, chunkCoords) -> {
            final Image img = new Image(region, worldTilesDir, mapWorld.config().ZOOM_MAX);

            final CompletableFuture<Void> future = CompletableFuture.allOf(chunkCoords.stream().map(coord ->
                    mapSingleChunk(img, coord.getX(), coord.getZ())).toArray(CompletableFuture[]::new));

            future.whenComplete((result, throwable) -> mapWorld.saveImage(img));
            futures.add(future);
        });
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            Logger.debug(String.format("Finished background render cycle in %.2f seconds",
                    (double) (System.currentTimeMillis() - time) / 1000.0D));
        }
    }
}
