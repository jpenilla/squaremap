package xyz.jpenilla.squaremap.common.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.data.Image;
import xyz.jpenilla.squaremap.common.data.TileCache;
import xyz.jpenilla.squaremap.common.data.TileUpdates;

@DefaultQualifier(NonNull.class)
public final class ImageIOExecutor {
    private static final int IMAGE_IO_MAX_TASKS = 100;
    private static final long FLUSH_INTERVAL_SECONDS = 2L;

    private final ScheduledExecutorService executor;
    private final TileCache tileCache;
    private final TileUpdates tileUpdates;
    private final AtomicLong submittedTasks = new AtomicLong();
    private final AtomicLong executedTasks = new AtomicLong();

    private ImageIOExecutor(final ServerLevel level, final TileCache tileCache, final TileUpdates tileUpdates) {
        this.executor = Executors.newSingleThreadScheduledExecutor(
            Util.squaremapThreadFactory("imageio", level)
        );
        this.tileCache = tileCache;
        this.tileUpdates = tileUpdates;
        // tiles shared by several regions are held back so they are only encoded once, so they
        // need writing out on a timer rather than as each region is drawn
        this.executor.scheduleWithFixedDelay(
            this::flush,
            FLUSH_INTERVAL_SECONDS,
            FLUSH_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    /**
     * Submits a save task for the given {@link Image} instance. If the save queue currently
     * has {@link #IMAGE_IO_MAX_TASKS} or more tasks queued, this method will block until the queue
     * has less than {@link #IMAGE_IO_MAX_TASKS} tasks. This effectively throttles renders when the
     * save queue falls far behind a render, avoiding a potential memory leak.
     *
     * @param image {@link Image} instance
     */
    public void saveImage(final Image image) {
        this.submittedTasks.getAndIncrement();
        this.executor.execute(() -> {
            try {
                image.drawTo(this.tileCache);
                this.tileUpdates.writeIfDue();
            } finally {
                this.executedTasks.getAndIncrement();
            }
        });

        long executed = this.executedTasks.get();
        long submitted = this.submittedTasks.get();
        for (int failures = 1; (submitted - executed) >= IMAGE_IO_MAX_TASKS; ++failures) {
            final boolean interrupted = Thread.interrupted();
            Thread.yield();
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(Math.min(25, failures)));
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            executed = this.executedTasks.get();
            submitted = this.submittedTasks.get();
        }
    }

    private void flush() {
        try {
            this.tileCache.flush();
            this.tileUpdates.writeIfDirty();
        } catch (final Exception ex) {
            // a failed flush must not cancel the scheduled task
            Logging.logger().warn("Failed to flush map tiles", ex);
        }
    }

    public void shutdown() {
        Util.shutdownExecutor(this.executor, TimeUnit.SECONDS, 5L);
        this.flush();
    }

    public static ImageIOExecutor create(final ServerLevel level, final TileCache tileCache, final TileUpdates tileUpdates) {
        return new ImageIOExecutor(level, tileCache, tileUpdates);
    }
}
