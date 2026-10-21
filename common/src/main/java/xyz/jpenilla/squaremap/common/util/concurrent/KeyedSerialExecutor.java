package xyz.jpenilla.squaremap.common.util.concurrent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * Runs tasks with the same key one at a time in submission order, and tasks with different keys concurrently.
 * Tasks waiting on their key are queued here rather than occupying a thread.
 *
 * @param <K> the key type
 */
@DefaultQualifier(NonNull.class)
public final class KeyedSerialExecutor<K> {
    private final Executor executor;
    // Present while a task for the key is running, holding the tasks submitted after it
    private final Map<K, Deque<Runnable>> waiting = new HashMap<>();

    /**
     * Creates an executor that runs tasks on the given executor.
     *
     * @param executor the executor to run tasks on, which must accept every task; a rejected task leaves its key stalled
     */
    public KeyedSerialExecutor(final Executor executor) {
        this.executor = executor;
    }

    /**
     * Runs the task once earlier tasks for its key have finished.
     *
     * @param key  the key to serialize on
     * @param task the task
     */
    public void execute(final K key, final Runnable task) {
        synchronized (this) {
            final @Nullable Deque<Runnable> queue = this.waiting.get(key);
            if (queue != null) {
                queue.add(task);
                return;
            }
            this.waiting.put(key, new ArrayDeque<>());
        }
        this.dispatch(key, task);
    }

    private void dispatch(final K key, final Runnable task) {
        this.executor.execute(() -> {
            try {
                task.run();
            } finally {
                final @Nullable Runnable next;
                synchronized (this) {
                    next = this.waiting.get(key).poll();
                    if (next == null) {
                        this.waiting.remove(key);
                    }
                }
                if (next != null) {
                    this.dispatch(key, next);
                }
            }
        });
    }
}
