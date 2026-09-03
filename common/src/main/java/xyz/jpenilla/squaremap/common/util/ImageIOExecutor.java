package xyz.jpenilla.squaremap.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.data.Image;
import xyz.jpenilla.squaremap.common.data.TileUpdates;

@DefaultQualifier(NonNull.class)
public final class ImageIOExecutor {
    private static final int IMAGE_IO_MAX_TASKS = 100;

    private final ExecutorService executor;
    private final TileUpdates tileUpdates;
    private final AtomicLong submittedTasks = new AtomicLong();
    private final AtomicLong executedTasks = new AtomicLong();

    private ImageIOExecutor(final ServerLevel level, final TileUpdates tileUpdates) {
        this.executor = Executors.newSingleThreadExecutor(
            Util.squaremapThreadFactory("imageio", level)
        );
        this.tileUpdates = tileUpdates;
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
                this.tileUpdates.record(image.save());
                if (this.submittedTasks.get() - this.executedTasks.get() == 1) {
                    // nothing else is queued, publish the manifest without waiting for the write interval
                    this.tileUpdates.writeIfDirty();
                } else {
                    this.tileUpdates.writeIfDue();
                }
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

    public void shutdown() {
        Util.shutdownExecutor(this.executor, TimeUnit.SECONDS, 5L);
        this.tileUpdates.writeIfDirty();
    }

    public static ImageIOExecutor create(final ServerLevel level, final TileUpdates tileUpdates) {
        return new ImageIOExecutor(level, tileUpdates);
    }
}
