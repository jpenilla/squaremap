package xyz.jpenilla.squaremap.common.render;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * Shares one snapshot load per key between overlapping scans. Each lease keeps its snapshots
 * available until it is closed; completed snapshots with no users are kept in a bounded cache.
 * Closing a lease or the cache does not cancel a snapshot load already submitted to the platform.
 *
 * @param <K> the snapshot key type
 * @param <V> the snapshot type
 */
@DefaultQualifier(NonNull.class)
final class SharedSnapshotCache<K, V> implements AutoCloseable {
    private final Map<K, Entry<V>> entries = new HashMap<>();
    private final LinkedHashMap<K, Entry<V>> idle = new LinkedHashMap<>();
    private final ExecutorService loaders = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore requests;
    private final int idleCapacity;
    private final RenderControl control;
    private final Function<K, CompletableFuture<@Nullable V>> load;
    private boolean closed;

    SharedSnapshotCache(
        final Semaphore requests,
        final int idleCapacity,
        final RenderControl control,
        final Function<K, CompletableFuture<@Nullable V>> load
    ) {
        this.requests = requests;
        this.idleCapacity = idleCapacity;
        this.control = control;
        this.load = load;
    }

    synchronized Lease acquire(final List<K> keys) throws InterruptedException {
        if (this.closed || this.control.cancelled()) {
            throw new InterruptedException("Snapshot registry closed");
        }
        final Map<K, Entry<V>> retained = new LinkedHashMap<>();
        for (final K key : keys) {
            if (retained.containsKey(key)) {
                continue;
            }
            Entry<V> entry = this.entries.get(key);
            if (entry == null) {
                entry = new Entry<>();
                this.entries.put(key, entry);
                final Entry<V> loading = entry;
                this.loaders.execute(() -> this.startLoad(key, loading));
            }
            this.idle.remove(key);
            entry.users++;
            retained.put(key, entry);
        }
        return new Lease(retained);
    }

    private void startLoad(final K key, final Entry<V> entry) {
        try {
            while (true) {
                this.control.await();
                this.requests.acquire();
                boolean submitted = false;
                try {
                    if (!this.control.canStartWork()) {
                        continue;
                    }
                    final CompletableFuture<@Nullable V> future = this.load.apply(key);
                    submitted = true;
                    future.whenComplete((value, failure) -> {
                        this.requests.release();
                        if (failure == null) {
                            entry.result.complete(value);
                        } else {
                            entry.result.completeExceptionally(failure);
                        }
                        this.loaded(key, entry);
                    });
                    return;
                } finally {
                    if (!submitted) {
                        this.requests.release();
                    }
                }
            }
        } catch (final Exception ex) {
            entry.result.completeExceptionally(ex);
            this.loaded(key, entry);
        }
    }

    private synchronized void loaded(final K key, final Entry<V> entry) {
        if (this.closed || this.entries.get(key) != entry) {
            return;
        }
        if (entry.users == 0) {
            this.idle.put(key, entry);
            this.evict();
        }
    }

    private void evict() {
        while (this.idle.size() > this.idleCapacity) {
            final Map.Entry<K, Entry<V>> oldest = this.idle.pollFirstEntry();
            if (oldest != null) {
                this.entries.remove(oldest.getKey(), oldest.getValue());
            }
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            this.closed = true;
            this.entries.clear();
            this.idle.clear();
        }
        this.loaders.shutdownNow();
        this.loaders.close();
    }

    final class Lease implements AutoCloseable {
        private final Map<K, Entry<V>> retained;
        private boolean released;

        private Lease(final Map<K, Entry<V>> retained) {
            this.retained = retained;
        }

        Map<K, @Nullable V> await(final List<K> keys) throws InterruptedException, ExecutionException {
            final Map<K, @Nullable V> snapshots = new HashMap<>();
            for (final K key : keys) {
                snapshots.put(key, this.retained.get(key).result.get());
            }
            return snapshots;
        }

        @Override
        public void close() {
            synchronized (SharedSnapshotCache.this) {
                if (this.released) {
                    return;
                }
                this.released = true;
                for (final Map.Entry<K, Entry<V>> retained : this.retained.entrySet()) {
                    final Entry<V> entry = retained.getValue();
                    if (--entry.users == 0) {
                        if (entry.result.isDone() && !SharedSnapshotCache.this.closed) {
                            SharedSnapshotCache.this.idle.put(retained.getKey(), entry);
                        }
                    }
                }
                SharedSnapshotCache.this.evict();
            }
        }
    }

    private static final class Entry<V> {
        private final CompletableFuture<@Nullable V> result = new CompletableFuture<>();
        private int users;
    }

}
