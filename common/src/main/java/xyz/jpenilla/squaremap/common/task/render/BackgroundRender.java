package xyz.jpenilla.squaremap.common.task.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.data.Image;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.Util;

public final class BackgroundRender extends AbstractRender {

    public BackgroundRender(final @NonNull MapWorldInternal world) {
        super(
            world,
            Executors.newFixedThreadPool(
                getThreads(world.config().BACKGROUND_RENDER_MAX_THREADS),
                Util.squaremapThreadFactory("bg-render-worker", world.serverLevel())
            )
        );
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
        while (this.mapWorld.hasModifiedChunks() && chunks.size() < this.mapWorld.config().BACKGROUND_RENDER_MAX_CHUNKS_PER_INTERVAL) {
            chunks.add(this.mapWorld.nextModifiedChunk());
        }
        final Map<RegionCoordinate, List<ChunkCoordinate>> coordMap = chunks.stream().collect(Collectors.groupingBy(ChunkCoordinate::regionCoordinate));

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        coordMap.forEach((region, chunkCoords) -> {
            final Image img = new Image(region, this.mapWorld.tilesPath(), this.mapWorld.config().ZOOM_MAX);

            final CompletableFuture<Void> future = CompletableFuture.allOf(chunkCoords.stream().map(coord ->
                mapSingleChunk(img, coord.x(), coord.z())).toArray(CompletableFuture[]::new));

            future.whenComplete((result, throwable) -> this.mapWorld.saveImage(img));
            futures.add(future);
        });
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            this.biomeColors.clear();
            Logging.debug(
                String.format(
                    "Finished background render cycle in %.2f seconds",
                    (double) (System.currentTimeMillis() - time) / 1000.0D
                )
            );
        }
    }
}
