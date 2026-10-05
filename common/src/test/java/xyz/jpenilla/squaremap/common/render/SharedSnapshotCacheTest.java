package xyz.jpenilla.squaremap.common.render;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import xyz.jpenilla.squaremap.common.world.SnapshotRequests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(10)
class SharedSnapshotCacheTest {
    @Test
    void resumeWakesWaiterWithoutHoldingCapacity() throws Exception {
        final RenderControl control = new RenderControl();
        final Semaphore requests = new Semaphore(1);
        final CountDownLatch started = new CountDownLatch(1);
        control.pause(true);
        try (final var cache = new SharedSnapshotCache<Integer, Integer>(requests, 0, control, key -> {
            started.countDown();
            return CompletableFuture.completedFuture(42);
        }); final var lease = cache.acquire(List.of(1))) {
            assertFalse(started.await(100, TimeUnit.MILLISECONDS));
            assertEquals(1, requests.availablePermits());
            control.pause(false);
            assertEquals(42, lease.await(List.of(1)).get(1));
        }
    }

    @Test
    void failedAdmissionReturnsCapacity() throws Exception {
        final Semaphore requests = new Semaphore(1);
        try (final var cache = new SharedSnapshotCache<Integer, String>(requests, 0, new RenderControl(), key -> {
            throw new IllegalStateException("submission failed");
        }); final var lease = cache.acquire(List.of(1))) {
            final ExecutionException failure = assertThrows(ExecutionException.class, () -> lease.await(List.of(1)));
            assertInstanceOf(IllegalStateException.class, failure.getCause());
            assertEquals("submission failed", failure.getCause().getMessage());
            assertEquals(1, requests.availablePermits());
        }
    }

    @Test
    void pauseDoesNotWaitForAdmittedOperationToReturn() throws Exception {
        final RenderControl control = new RenderControl();
        final Semaphore requests = new Semaphore(1);
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        try (final var cache = new SharedSnapshotCache<Integer, Integer>(requests, 0, control, key -> {
            entered.countDown();
            try {
                release.await();
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
            return CompletableFuture.completedFuture(42);
        }); final var lease = cache.acquire(List.of(1)); final var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                tasks.submit(() -> control.pause(true)).get(5, TimeUnit.SECONDS);
            } finally {
                release.countDown();
            }
            assertEquals(42, lease.await(List.of(1)).get(1));
            assertEquals(1, requests.availablePermits());
        }
    }

    @Test
    void failedDependencyDoesNotHideOtherResults() throws Exception {
        try (final var cache = new SharedSnapshotCache<Integer, String>(new Semaphore(2), 0, new RenderControl(), key ->
            key == 1 ? CompletableFuture.failedFuture(new IllegalStateException("read failed")) : CompletableFuture.completedFuture("snapshot")
        ); final var lease = cache.acquire(List.of(1, 2))) {
            final ExecutionException failure = assertThrows(ExecutionException.class, () -> lease.await(List.of(1)));
            assertEquals("read failed", failure.getCause().getMessage());
            assertEquals("snapshot", lease.await(List.of(2)).get(2));
        }
    }

    @Test
    void sharedLoadSurvivesConsumerReleaseAndIdleEviction() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final CompletableFuture<String> platform = new CompletableFuture<>();
        final CountDownLatch started = new CountDownLatch(1);
        try (final var cache = new SharedSnapshotCache<Integer, String>(new Semaphore(1), 0, new RenderControl(), key -> {
            calls.incrementAndGet();
            started.countDown();
            return platform;
        })) {
            final var first = cache.acquire(List.of(1));
            try (final var second = cache.acquire(List.of(1))) {
                assertTrue(started.await(1, TimeUnit.SECONDS));
                first.close();
                assertFalse(platform.isCancelled());
                platform.complete("snapshot");
                assertEquals("snapshot", second.await(List.of(1)).get(1));
                try (final var third = cache.acquire(List.of(1))) {
                    assertEquals("snapshot", third.await(List.of(1)).get(1));
                }
                assertEquals(1, calls.get());
            }
            try (final var reload = cache.acquire(List.of(1))) {
                assertEquals("snapshot", reload.await(List.of(1)).get(1));
                assertEquals(2, calls.get());
            }
        }
    }

    @Test
    void closingCacheLeavesOutstandingLoadsHoldingCapacityAcrossSessions() throws Exception {
        final SnapshotRequests requests = new SnapshotRequests(1);
        final CompletableFuture<String> oldLoad = new CompletableFuture<>();
        final CountDownLatch oldStarted = new CountDownLatch(1);
        final CountDownLatch newStarted = new CountDownLatch(1);
        final var old = new SharedSnapshotCache<Integer, String>(requests, 0, new RenderControl(), key -> {
            oldStarted.countDown();
            return oldLoad;
        });
        try (final var oldLease = old.acquire(List.of(1))) {
            assertTrue(oldStarted.await(5, TimeUnit.SECONDS));
            old.close();
            assertFalse(oldLoad.isDone());
            try (final var next = new SharedSnapshotCache<Integer, String>(requests, 0, new RenderControl(), key -> {
                newStarted.countDown();
                return CompletableFuture.completedFuture("new");
            }); final var nextLease = next.acquire(List.of(2))) {
                assertFalse(newStarted.await(100, TimeUnit.MILLISECONDS));
                oldLoad.complete("old");
                assertEquals("new", nextLease.await(List.of(2)).get(2));
                assertEquals("old", oldLease.await(List.of(1)).get(1));
            }
        } finally {
            old.close();
        }
    }
}
