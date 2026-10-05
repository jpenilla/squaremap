package xyz.jpenilla.squaremap.common.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.chunksnapshot.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.chunksnapshot.ChunkSnapshotProviderFactory;
import xyz.jpenilla.squaremap.common.config.RenderConfig;
import xyz.jpenilla.squaremap.common.coordinate.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.render.output.RegionImage;
import xyz.jpenilla.squaremap.common.render.scanning.ChunkScanner;
import xyz.jpenilla.squaremap.common.render.scanning.ScanSettings;
import xyz.jpenilla.squaremap.common.server.ServerAccess;
import xyz.jpenilla.squaremap.common.util.concurrent.Threads;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;

/**
 * Loads chunk snapshots, scans them on CPU workers, and queues region images for saving.
 */
@DefaultQualifier(NonNull.class)
public final class RenderJob {
    private final MapWorldInternal world;
    private final ServerAccess server;
    private final RenderPlan plan;
    private final @Nullable RenderCheckpointStore checkpoints;
    private final Mode mode;
    private final RenderConfig limits;
    private final ScanSettings settings;
    private final int maxZoom;
    private final RenderControl control = new RenderControl();
    private final ExecutorService tasks = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService cpu;
    private final SharedSnapshotCache<ChunkCoordinate, ChunkSnapshot> snapshots;
    private final Semaphore chunkSlots;
    private final ThreadLocal<ChunkScanner> scanners;
    private final int totalChunks;
    private final int totalRegions;
    private final AtomicInteger processedChunks = new AtomicInteger();
    private final AtomicInteger processedRegions = new AtomicInteger();
    private @Nullable RenderProgressReporter progress;

    RenderJob(
        final MapWorldInternal world,
        final ChunkSnapshotProviderFactory snapshots,
        final ServerAccess server,
        final Mode mode,
        final RenderConfig limits,
        final ScanSettings settings,
        final RenderPlan plan,
        final @Nullable RenderCheckpointStore checkpoints
    ) {
        this.world = world;
        this.server = server;
        this.plan = plan;
        this.checkpoints = checkpoints;
        this.mode = mode;
        this.limits = limits;
        this.settings = settings;
        this.maxZoom = world.config().ZOOM_MAX;
        this.totalChunks = plan.regions().stream().mapToInt(RenderPlan.RegionWork::chunkCount).sum();
        this.totalRegions = mode.foreground() ? plan.regions().size() : -1;
        this.processedChunks.set(plan.regions().stream().filter(RenderPlan.RegionWork::completed).mapToInt(RenderPlan.RegionWork::chunkCount).sum());
        this.processedRegions.set((int) plan.regions().stream().filter(RenderPlan.RegionWork::completed).count());
        this.chunkSlots = new Semaphore(limits.maxActiveChunks);
        this.cpu = Executors.newFixedThreadPool(limits.workers, Threads.squaremapThreadFactory("render-worker", world.serverLevel()));
        this.scanners = ThreadLocal.withInitial(() -> new ChunkScanner(
            world.blockColors(),
            world.biomeColorTables(),
            world.serverLevel().getBiomeManager(),
            world.serverLevel()::getUncachedNoiseBiome,
            this.settings,
            this.control
        ));
        final var provider = snapshots.createChunkSnapshotProvider(world.serverLevel());
        this.snapshots = new SharedSnapshotCache<>(world.snapshotRequests(), limits.idleChunkCacheSize, this.control,
            coordinate -> provider.asyncSnapshot(coordinate.x(), coordinate.z()));
    }

    /**
     * Executes the job once. Unfinished background work is returned to the world's dirty chunks,
     * including when the job was stopped before it started.
     *
     * @return the outcome
     */
    RenderOutcome execute() {
        boolean sleepBlocked = false;
        RenderOutcome outcome = RenderOutcome.FAILED;
        try {
            if (this.control.requested() != null) {
                throw new InterruptedException("Render stopped before execution");
            }
            this.server.blockSleep();
            sleepBlocked = true;
            if (this.checkpoints != null) {
                this.checkpoints.save(this.mode, this.plan);
            }
            if (this.mode.foreground()) {
                this.startProgress();
            }
            this.world.snapshotRequests().limit(this.limits.maxChunkRequests);
            this.render();
            outcome = RenderOutcome.COMPLETED;
        } catch (final InterruptedException ignore) {
            outcome = RenderOutcome.STOPPED;
        } catch (final Exception ex) {
            if (this.control.requested() == null) {
                Logging.logger().warn("Encountered exception executing {} render for {}", this.mode, this.world.identifier().asString(), ex);
            }
        } finally {
            try {
                if (outcome != RenderOutcome.COMPLETED) {
                    this.control.cancel();
                }
                this.close();
            } finally {
                this.stopProgress();
                try {
                    if (!this.mode.foreground()) {
                        this.requeueUnfinished();
                    }
                } finally {
                    if (sleepBlocked) {
                        this.server.allowSleep();
                    }
                }
            }
        }
        final RenderOutcome requested = this.control.requested();
        return requested == null ? outcome : requested;
    }

    private synchronized void stopProgress() {
        final RenderProgressReporter progress = this.progress;
        if (progress != null) {
            progress.close();
            this.progress = null;
        }
    }

    private synchronized void startProgress() {
        this.progress = new RenderProgressReporter(this);
    }

    synchronized void restartProgressLogger() {
        final RenderProgressReporter progress = this.progress;
        if (progress != null) {
            progress.restartLogging();
        }
    }

    MapWorldInternal world() {
        return this.world;
    }

    int totalChunks() {
        return this.totalChunks;
    }

    int totalRegions() {
        return this.totalRegions;
    }

    int processedChunks() {
        return this.processedChunks.get();
    }

    int processedRegions() {
        return this.processedRegions.get();
    }

    void pause(final boolean paused) {
        this.control.pause(paused);
    }

    boolean paused() {
        return this.control.paused();
    }

    void stop(final RenderOutcome outcome) {
        this.control.stop(outcome);
    }

    private void render() throws InterruptedException, ExecutionException {
        final ExecutorCompletionService<RenderPlan.RegionWork> results = new ExecutorCompletionService<>(this.tasks);
        int active = 0;
        for (final RenderPlan.RegionWork work : this.plan.regions()) {
            if (work.completed()) {
                continue;
            }
            if (active == this.limits.maxActiveRegions) {
                this.regionFinished(results.take().get());
                active--;
            }
            this.control.await();
            results.submit(() -> {
                this.renderRegion(work);
                return work;
            });
            active++;
        }
        while (active > 0) {
            this.regionFinished(results.take().get());
            active--;
        }
    }

    private void regionFinished(final RenderPlan.RegionWork work) {
        // Region completion tracks queued images, not finished disk writes.
        work.complete();
        this.processedRegions.incrementAndGet();
        if (this.checkpoints != null) {
            this.checkpoints.save(this.mode, this.plan);
        }
    }

    private void renderRegion(final RenderPlan.RegionWork work) throws InterruptedException, ExecutionException {
        final RegionImage image = new RegionImage(work.coordinate(), this.world.tilesPath(), this.maxZoom);
        final ExecutorCompletionService<Integer> results = new ExecutorCompletionService<>(this.tasks);
        final int columnChunks = Math.min(this.limits.maxActiveChunks, this.limits.maxColumnChunks);
        int active = 0;
        for (final List<RenderPlan.ChunkWork> column : work.columns(columnChunks)) {
            while (active + column.size() > this.limits.maxActiveChunks) {
                active -= results.take().get();
            }
            this.control.await();
            this.chunkSlots.acquire(column.size());
            try {
                results.submit(() -> {
                    try {
                        return this.renderColumn(column, image);
                    } finally {
                        this.chunkSlots.release(column.size());
                    }
                });
            } catch (final RuntimeException ex) {
                this.chunkSlots.release(column.size());
                throw ex;
            }
            active += column.size();
        }
        while (active > 0) {
            active -= results.take().get();
        }
        if (this.control.cancelled()) {
            throw new InterruptedException("Render cancelled");
        }
        this.world.saveImage(image);
    }

    private int renderColumn(final List<RenderPlan.ChunkWork> column, final RegionImage image) throws InterruptedException, ExecutionException {
        final List<List<ChunkCoordinate>> dependencies = column.stream().map(this::dependencies).toList();
        final var lease = this.snapshots.acquire(dependencies.stream().flatMap(List::stream).toList());
        final Future<Integer> scan;
        try {
            final List<ScanInput> inputs = this.awaitInputs(column, dependencies, lease);
            this.control.await();
            scan = this.cpu.submit(() -> {
                try (lease) {
                    return this.scanColumn(inputs, image);
                }
            });
        } catch (final Exception ex) {
            lease.close();
            throw ex;
        }
        return scan.get();
    }

    private List<ScanInput> awaitInputs(
        final List<RenderPlan.ChunkWork> column,
        final List<List<ChunkCoordinate>> dependencies,
        final SharedSnapshotCache<ChunkCoordinate, ChunkSnapshot>.Lease lease
    ) throws InterruptedException {
        final List<ScanInput> inputs = new ArrayList<>(column.size());
        for (int i = 0; i < column.size(); i++) {
            final RenderPlan.ChunkWork work = column.get(i);
            try {
                inputs.add(new ScanInput(work, lease.await(dependencies.get(i))));
            } catch (final ExecutionException ex) {
                this.chunkFailed(work, ex);
                inputs.add(new ScanInput(work, null));
            }
        }
        return inputs;
    }

    private int scanColumn(final List<ScanInput> inputs, final RegionImage image) throws InterruptedException {
        final ChunkScanner scanner = this.scanners.get();
        for (final ScanInput input : inputs) {
            this.control.await();
            if (input.snapshots() != null) {
                try {
                    scanner.scan(input.work(), input.snapshots(), image);
                } catch (final Exception ex) {
                    this.chunkFailed(input.work(), ex);
                }
            }
            if (!input.work().topRowOnly()) {
                this.processedChunks.incrementAndGet();
            }
        }
        return inputs.size();
    }

    private void chunkFailed(final RenderPlan.ChunkWork work, final Exception failure) throws InterruptedException {
        final Throwable cause = failure instanceof ExecutionException && failure.getCause() != null ? failure.getCause() : failure;
        if (this.control.cancelled() || cause instanceof CancellationException || cause instanceof InterruptedException) {
            throw new InterruptedException("Render cancelled");
        }
        Logging.logger().warn("Exception mapping chunk at [{}, {}] in {}", work.coordinate().x(), work.coordinate().z(), this.world.identifier().asString(), cause);
    }

    private record ScanInput(RenderPlan.ChunkWork work, @Nullable Map<ChunkCoordinate, @Nullable ChunkSnapshot> snapshots) {
    }

    private List<ChunkCoordinate> dependencies(final RenderPlan.ChunkWork work) {
        final ChunkCoordinate target = work.coordinate();
        final List<ChunkCoordinate> result = new ArrayList<>();
        result.add(target);
        result.add(new ChunkCoordinate(target.x(), target.z() - 1));
        if (this.settings.biomes()) {
            final int margin = this.settings.biomeMargin();
            final int minX = Math.floorDiv(target.getBlockX() - margin, 16);
            final int maxX = Math.floorDiv(target.getBlockX() + 15 + margin, 16);
            final int minZ = Math.floorDiv(target.getBlockZ() - margin, 16);
            final int maxZ = Math.floorDiv(target.getBlockZ() + (work.topRowOnly() ? 0 : 15) + margin, 16);
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    result.add(new ChunkCoordinate(x, z));
                }
            }
        }
        return result;
    }

    private void requeueUnfinished() {
        for (final RenderPlan.RegionWork region : this.plan.regions()) {
            if (!region.completed()) {
                for (final RenderPlan.ChunkWork chunk : region.chunks()) {
                    this.world.chunkModified(chunk.coordinate());
                }
            }
        }
    }

    private void close() {
        if (this.control.cancelled()) {
            this.tasks.shutdownNow();
        }
        this.tasks.close();
        // Wait for scans to release their snapshot leases before closing the cache.
        this.cpu.shutdown();
        Threads.awaitTerminationUninterruptibly(this.cpu);
        this.snapshots.close();
    }

    enum Mode {
        FULL(true),
        RADIUS(true),
        BACKGROUND(false);

        private final boolean foreground;

        Mode(final boolean foreground) {
            this.foreground = foreground;
        }

        boolean foreground() {
            return this.foreground;
        }
    }
}
