package xyz.jpenilla.squaremap.common.task.render;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.data.Image;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshotProviderFactory;

@DefaultQualifier(NonNull.class)
public final class BackgroundRender extends AbstractRender {
    // how many cycles a chunk that could not be read is retried for before it is given up on.
    // a freshly generated chunk can briefly be readable from neither memory nor disk, but a
    // chunk that simply does not exist must not be requeued forever
    private static final int MAX_ATTEMPTS = 3;

    private final ServerAccess serverAccess;
    private final Map<ChunkCoordinate, Integer> failedAttempts = new HashMap<>();

    @AssistedInject
    private BackgroundRender(
        @Assisted final MapWorldInternal world,
        final ChunkSnapshotProviderFactory chunkSnapshotProviderFactory,
        final ServerAccess serverAccess
    ) {
        super(world, chunkSnapshotProviderFactory, createBackgroundRenderWorkerPool(world));
        this.serverAccess = serverAccess;
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
        try {
            this.serverAccess.blockSleep();
            this.render(time, chunks);
        } finally {
            this.serverAccess.allowSleep();
        }
    }

    private void render(final long time, final Set<ChunkCoordinate> chunks) {
        final List<CompletableFuture<Void>> regionFutures = new ArrayList<>();

        final Map<RegionCoordinate, List<ChunkCoordinate>> regionChunksMap = chunks.stream().collect(Collectors.groupingBy(ChunkCoordinate::regionCoordinate));
        regionChunksMap.forEach((region, chunksToRenderInRegion) -> {
            final Image image = new Image(region, this.mapWorld.config().ZOOM_MAX);

            final Map<ChunkCoordinate, CompletableFuture<Boolean>> chunkFutures = new LinkedHashMap<>();
            for (final ChunkCoordinate coord : chunksToRenderInRegion) {
                chunkFutures.put(coord, this.mapSingleChunk(image, coord.x(), coord.z()));
            }

            regionFutures.add(CompletableFuture.allOf(chunkFutures.values().toArray(CompletableFuture<?>[]::new)).thenRun(() -> {
                if (!this.running()) {
                    return;
                }
                // only drop the chunks that were actually drawn. one that could not be read
                // contributed nothing to the image, so leaving it queued lets it be retried
                // instead of leaving a permanent hole in the map
                chunkFutures.forEach((coord, future) -> {
                    if (future.join()) {
                        this.failedAttempts.remove(coord);
                        chunks.remove(coord);
                    }
                });
                this.mapWorld.saveImage(image);
            }));
        });

        try {
            CompletableFuture.allOf(regionFutures.toArray(CompletableFuture<?>[]::new)).get();
        } catch (final InterruptedException ignore) {
        } catch (final CancellationException | ExecutionException ex) {
            Logging.logger().error("Exception executing background render", ex);
        }

        this.clearCaches();

        this.requeueFailed(chunks);

        Logging.debug(() -> String.format(
            "Finished background render cycle in %.2f seconds",
            (double) (System.currentTimeMillis() - time) / 1000.0D
        ));
    }

    /**
     * Requeues chunks that could not be read this cycle, giving up on any that have failed
     * {@link #MAX_ATTEMPTS} times so that chunks which will never be readable do not
     * accumulate in the queue.
     *
     * @param chunks chunks left unrendered
     */
    private void requeueFailed(final Set<ChunkCoordinate> chunks) {
        final Iterator<ChunkCoordinate> it = chunks.iterator();
        while (it.hasNext()) {
            final ChunkCoordinate coord = it.next();
            if (this.failedAttempts.merge(coord, 1, Integer::sum) >= MAX_ATTEMPTS) {
                this.failedAttempts.remove(coord);
                it.remove();
            }
        }
        chunks.forEach(this.mapWorld::chunkModified);
    }

    private static ExecutorService createBackgroundRenderWorkerPool(final MapWorldInternal world) {
        return Util.newFixedThreadPool(
            getThreads(world.config().BACKGROUND_RENDER_MAX_THREADS, 3),
            Util.squaremapThreadFactory("bg-render-worker", world.serverLevel()),
            new ThreadPoolExecutor.DiscardPolicy()
        );
    }
}
