package xyz.jpenilla.squaremap.plugin.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * Same as DefaultThreadFactory, with a custom name prefix.
 */
@DefaultQualifier(NonNull.class)
public final class NamedThreadFactory implements ThreadFactory {
    private final AtomicInteger threadCount = new AtomicInteger(1);
    private final String namePrefix;

    public NamedThreadFactory(final String poolName) {
        this.namePrefix = poolName + "-";
    }

    public Thread newThread(final Runnable runnable) {
        final Thread thread = new Thread(
            null,
            runnable,
            this.namePrefix + this.threadCount.getAndIncrement(),
            0
        );
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }
}
