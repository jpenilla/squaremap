package xyz.jpenilla.squaremap.common.render.output;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.ImageSavingConfig;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.concurrent.Threads;

@DefaultQualifier(NonNull.class)
public final class RegionImageSaveQueue {
    private final ExecutorService executor;
    private final Semaphore capacity;
    private final Map<Path, CompletableFuture<Void>> fileTails = new HashMap<>();

    RegionImageSaveQueue(final int workers, final int maxPendingImages, final ThreadFactory factory) {
        this.executor = Executors.newFixedThreadPool(workers, factory);
        this.capacity = new Semaphore(maxPendingImages);
    }

    /**
     * Waits for queue capacity and schedules the image's writes without waiting for completion.
     * Its pixels must not be modified after this call because the writes use them asynchronously.
     *
     * @param image the image to save
     * @throws InterruptedException if interrupted while waiting for queue capacity
     */
    public void saveImage(final RegionImage image) throws InterruptedException {
        this.capacity.acquire();
        try {
            this.savePerFile(image);
        } catch (final RuntimeException ex) {
            this.capacity.release();
            throw ex;
        }
    }

    private synchronized void savePerFile(final RegionImage image) {
        final List<CompletableFuture<Void>> writes = new ArrayList<>();
        for (final var task : image.saveTasks().entrySet()) {
            final Path file = task.getKey();
            final CompletableFuture<Void> fileTail = this.fileTails.getOrDefault(file, CompletableFuture.completedFuture(null));
            final CompletableFuture<Void> next = fileTail.thenRunAsync(() -> {
                try {
                    task.getValue().run();
                } catch (final Throwable ex) {
                    Logging.error(Messages.LOG_COULD_NOT_SAVE_IMAGE, ex, "path", file);
                }
            }, this.executor);
            this.fileTails.put(file, next);
            next.whenComplete((result, error) -> {
                synchronized (this) {
                    this.fileTails.remove(file, next);
                }
            });
            writes.add(next);
        }
        CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new))
            .whenComplete((result, error) -> this.capacity.release());
    }

    /**
     * Waits for saves already queued when this method is called. Interruptions do not stop
     * the wait; the caller's interrupt status is restored when the wait finishes.
     *
     * @throws IllegalStateException if the image save barrier fails
     */
    public void drain() {
        final Future<?> barrier;
        synchronized (this) {
            barrier = CompletableFuture.allOf(this.fileTails.values().toArray(CompletableFuture[]::new));
        }
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    barrier.get();
                    return;
                } catch (final InterruptedException ignore) {
                    interrupted = true;
                } catch (final ExecutionException ex) {
                    throw new IllegalStateException("Image save barrier failed", ex);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void shutdown() {
        this.drain();
        this.executor.close();
    }

    public static RegionImageSaveQueue create(final ServerLevel level, final ImageSavingConfig config) {
        return new RegionImageSaveQueue(config.workers, config.maxPendingImages, Threads.squaremapThreadFactory("imageio", level));
    }
}
