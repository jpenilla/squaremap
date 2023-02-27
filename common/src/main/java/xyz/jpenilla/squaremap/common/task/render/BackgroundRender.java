package xyz.jpenilla.squaremap.common.task.render;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.data.Image;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshotProviderFactory;

@DefaultQualifier(NonNull.class)
public final class BackgroundRender extends AbstractRender {
    @AssistedInject
    private BackgroundRender(
        @Assisted final MapWorldInternal world,
        final ChunkSnapshotProviderFactory chunkSnapshotProviderFactory
    ) {
        super(world, chunkSnapshotProviderFactory, createBackgroundRenderWorkerPool(world));
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
        final long time = System.currentTimeMillis();
        final Set<ChunkCoordinate> chunks = ConcurrentHashMap.newKeySet();
        while (this.mapWorld.hasModifiedChunks() && chunks.size() < this.mapWorld.config().BACKGROUND_RENDER_MAX_CHUNKS_PER_INTERVAL) {
            chunks.add(this.mapWorld.nextModifiedChunk());
        }
        if (chunks.isEmpty()) {
            return;
        }

        final List<CompletableFuture<Void>> regionFutures = new ArrayList<>();

        final Map<RegionCoordinate, List<ChunkCoordinate>> regionChunksMap = chunks.stream().collect(Collectors.groupingBy(ChunkCoordinate::regionCoordinate));
        regionChunksMap.forEach((region, chunksToRenderInRegion) -> {
            final Image image = new Image(region, this.mapWorld.tilesPath(), this.mapWorld.config().ZOOM_MAX);

            final CompletableFuture<?>[] chunkFutures = chunksToRenderInRegion.stream()
                .map(coord -> this.mapSingleChunkFuture(image, coord.x(), coord.z()))
                .toArray(CompletableFuture<?>[]::new);

            regionFutures.add(CompletableFuture.allOf(chunkFutures).thenRun(() -> {
                if (!this.running()) {
                    return;
                }
                chunksToRenderInRegion.forEach(chunks::remove);
                this.mapWorld.saveImage(image);
            }));
        });

        try {
            CompletableFuture.allOf(regionFutures.toArray(CompletableFuture<?>[]::new)).get();
        } catch (final InterruptedException ignore) {
        } catch (final CancellationException | ExecutionException ex) {
            Logging.logger().error("Exception executing background render", ex);
        }

        if (this.biomeColors != null) {
            this.biomeColors.clear();
        }
        this.resetChunkSnapshotProvider();

        chunks.forEach(this.mapWorld::chunkModified);

        Logging.debug(() -> String.format(
            "Finished background render cycle in %.2f seconds",
            (double) (System.currentTimeMillis() - time) / 1000.0D
        ));
    }

    private static ExecutorService createBackgroundRenderWorkerPool(final MapWorldInternal world) {
        return Util.newFixedThreadPool(
            getThreads(world.config().BACKGROUND_RENDER_MAX_THREADS, 3),
            Util.squaremapThreadFactory("bg-render-worker", world.serverLevel()),
            new ThreadPoolExecutor.DiscardPolicy()
        );
    }
}
