package xyz.jpenilla.squaremap.common.util.concurrent;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;

/**
 * Runs tasks one at a time, in submission order, on a single virtual thread. Exceptions thrown by
 * tasks are logged. After shutdown, new tasks and tasks still waiting on a delay are discarded.
 */
@DefaultQualifier(NonNull.class)
public final class BackgroundExecutor {
    private final ScheduledThreadPoolExecutor executor;

    /**
     * Creates an executor with its own thread.
     *
     * @param threadName the name of the executor's thread
     */
    public BackgroundExecutor(final String threadName) {
        this.executor = new ScheduledThreadPoolExecutor(1, Threads.virtualThread(threadName), new ThreadPoolExecutor.DiscardPolicy());
        this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    public void execute(final Runnable task) {
        this.executor.execute(() -> run(task));
    }

    public void schedule(final Runnable task, final long delay, final TimeUnit unit) {
        this.executor.schedule(() -> run(task), delay, unit);
    }

    /**
     * Stops accepting tasks and waits for the ones already submitted without a pending delay.
     */
    public void shutdown() {
        this.executor.shutdown();
        Threads.awaitTerminationUninterruptibly(this.executor);
    }

    private static void run(final Runnable task) {
        try {
            task.run();
        } catch (final RuntimeException ex) {
            Logging.logger().error("Exception in background task", ex);
        }
    }
}
