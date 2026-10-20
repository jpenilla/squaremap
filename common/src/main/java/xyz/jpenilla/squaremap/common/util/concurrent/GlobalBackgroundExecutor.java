package xyz.jpenilla.squaremap.common.util.concurrent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * The {@link BackgroundExecutor} for work not tied to one world, running between
 * {@link #start()} and {@link #shutdown()}. Tasks submitted while stopped are discarded.
 */
@DefaultQualifier(NonNull.class)
@Singleton
public final class GlobalBackgroundExecutor {
    private volatile @Nullable BackgroundExecutor executor;

    @Inject
    private GlobalBackgroundExecutor() {
    }

    public void start() {
        this.executor = new BackgroundExecutor(Threads.name("background"));
    }

    public void execute(final Runnable task) {
        final @Nullable BackgroundExecutor executor = this.executor;
        if (executor != null) {
            executor.execute(task);
        }
    }

    /**
     * Stops accepting tasks and waits for the ones already submitted.
     */
    public void shutdown() {
        final @Nullable BackgroundExecutor executor = this.executor;
        this.executor = null;
        if (executor != null) {
            executor.shutdown();
        }
    }
}
