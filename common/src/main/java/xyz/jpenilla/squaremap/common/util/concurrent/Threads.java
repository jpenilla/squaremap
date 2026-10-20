package xyz.jpenilla.squaremap.common.util.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

@DefaultQualifier(NonNull.class)
public final class Threads {
    private Threads() {
    }

    public static void throwIfInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Thread interrupted");
        }
    }

    /**
     * Builds a thread name.
     *
     * @param role what the thread does
     * @return {@code squaremap-<role>}
     */
    public static String name(final String role) {
        return "squaremap-" + role;
    }

    /**
     * Builds a thread name for a thread working on one world.
     *
     * @param role  what the thread does
     * @param world the world
     * @return {@code squaremap-<role>-[<world>]}
     */
    public static String name(final String role, final WorldIdentifier world) {
        return name(role) + "-[" + world.asString() + "]";
    }

    /**
     * Creates a factory for non-daemon platform threads of normal priority, numbered from 1.
     *
     * @param name the thread name, before the number
     * @return the factory
     */
    public static ThreadFactory platformThreads(final String name) {
        return Thread.ofPlatform().name(name + "-", 1).daemon(false).priority(Thread.NORM_PRIORITY).factory();
    }

    /**
     * Creates a factory for virtual threads that all share one name, for executors with a single thread.
     *
     * @param name the thread name
     * @return the factory
     */
    public static ThreadFactory virtualThread(final String name) {
        return Thread.ofVirtual().name(name).factory();
    }

    /**
     * Creates a factory for virtual threads, numbered from 1.
     *
     * @param name the thread name, before the number
     * @return the factory
     */
    public static ThreadFactory virtualThreads(final String name) {
        return Thread.ofVirtual().name(name + "-", 1).factory();
    }

    /**
     * Waits for the latch, restoring the interrupt status afterward if interrupted while waiting.
     *
     * @param latch the latch
     */
    public static void awaitUninterruptibly(final CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                latch.await();
                break;
            } catch (final InterruptedException ignore) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Waits for a shut-down executor to terminate, restoring the interrupt status afterward if
     * interrupted while waiting.
     *
     * @param executor the executor
     */
    public static void awaitTerminationUninterruptibly(final ExecutorService executor) {
        boolean interrupted = false;
        while (true) {
            try {
                if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                    break;
                }
            } catch (final InterruptedException ignore) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
