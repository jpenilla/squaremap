package xyz.jpenilla.squaremap.common.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;

@DefaultQualifier(NonNull.class)
public final class ExceptionLoggingScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {
    public ExceptionLoggingScheduledThreadPoolExecutor(final int corePoolSize, final ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return super.schedule(new ExceptionLoggingRunnable(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
        return super.scheduleAtFixedRate(new ExceptionLoggingRunnable(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
        return super.scheduleWithFixedDelay(new ExceptionLoggingRunnable(command), initialDelay, delay, unit);
    }

    private record ExceptionLoggingRunnable(Runnable wrapped) implements Runnable {
        @Override
        public void run() {
            try {
                this.wrapped.run();
            } catch (final Throwable thr) {
                Logging.logger().error("Error executing task '{}'", this.wrapped, thr);
                Util.rethrow(thr);
            }
        }
    }
}
