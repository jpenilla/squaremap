package xyz.jpenilla.squaremap.common.util.concurrent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(10)
class KeyedSerialExecutorTest {
    @Test
    void sameKeyRunsInOrderOneAtATime() throws Exception {
        final List<Integer> ran = new CopyOnWriteArrayList<>();
        final CountDownLatch done = new CountDownLatch(100);
        final AtomicInteger running = new AtomicInteger();
        final AtomicBoolean overlapped = new AtomicBoolean();
        try (final ExecutorService pool = Executors.newFixedThreadPool(4)) {
            final KeyedSerialExecutor<String> executor = new KeyedSerialExecutor<>(pool);
            for (int i = 0; i < 100; i++) {
                final int task = i;
                executor.execute("key", () -> {
                    if (running.incrementAndGet() != 1) {
                        overlapped.set(true);
                    }
                    ran.add(task);
                    running.decrementAndGet();
                    done.countDown();
                });
            }
            assertTrue(done.await(5, TimeUnit.SECONDS));
        }
        assertEquals(IntStream.range(0, 100).boxed().toList(), ran);
        assertFalse(overlapped.get());
    }

    @Test
    void waitingTaskDoesNotHoldAThread() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch otherKeyRan = new CountDownLatch(1);
        final CountDownLatch queuedRan = new CountDownLatch(1);
        try (final ExecutorService pool = Executors.newFixedThreadPool(2)) {
            final KeyedSerialExecutor<String> executor = new KeyedSerialExecutor<>(pool);
            executor.execute("a", () -> {
                try {
                    release.await();
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            executor.execute("a", queuedRan::countDown);
            executor.execute("b", otherKeyRan::countDown);
            assertTrue(otherKeyRan.await(5, TimeUnit.SECONDS));
            release.countDown();
            assertTrue(queuedRan.await(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void failedTaskDoesNotStopLaterTasks() throws Exception {
        final CountDownLatch ran = new CountDownLatch(1);
        try (final ExecutorService pool = Executors.newFixedThreadPool(1)) {
            final KeyedSerialExecutor<String> executor = new KeyedSerialExecutor<>(pool);
            executor.execute("key", () -> {
                throw new IllegalStateException("expected");
            });
            executor.execute("key", ran::countDown);
            assertTrue(ran.await(5, TimeUnit.SECONDS));
        }
    }
}
