package xyz.jpenilla.squaremap.common.util.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class Threads {
    private Threads() {
    }

    public static ThreadFactory squaremapThreadFactory(final String name) {
        return new NamedThreadFactory("squaremap-" + name);
    }

    public static ThreadFactory squaremapThreadFactory(final String name, final ServerLevel level) {
        return squaremapThreadFactory(name + "-[" + level.dimension().identifier() + "]");
    }

    public static ThreadPoolExecutor newFixedThreadPool(final int size, final ThreadFactory threadFactory, final RejectedExecutionHandler rejectedExecutionHandler) {
        return new ThreadPoolExecutor(
            size,
            size,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            threadFactory,
            rejectedExecutionHandler
        );
    }

    public static void shutdownExecutor(final ExecutorService service, final TimeUnit timeoutUnit, final long timeoutLength) {
        service.shutdown();
        boolean didShutdown;
        try {
            didShutdown = service.awaitTermination(timeoutLength, timeoutUnit);
        } catch (final InterruptedException ignore) {
            didShutdown = false;
        }
        if (!didShutdown) {
            service.shutdownNow();
        }
    }
}
