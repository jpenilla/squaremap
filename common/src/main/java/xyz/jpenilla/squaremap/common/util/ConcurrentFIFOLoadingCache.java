package xyz.jpenilla.squaremap.common.util;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ConcurrentFIFOLoadingCache<K, V> {
    private final int maximumCapacity;
    private final int evictUntil;
    private final Function<K, V> loader;
    // lookup map
    private final Map<K, V> map;
    // FIFO eviction queue
    private final Queue<K> queue;

    public ConcurrentFIFOLoadingCache(
        final int maximumCapacity,
        final int evictUntil,
        final Function<K, V> loader
    ) {
        if (maximumCapacity <= evictUntil) {
            throw new IllegalArgumentException("maximumCapacity must be larger than evictUntil (%s <= %s)".formatted(maximumCapacity, evictUntil));
        }
        this.maximumCapacity = maximumCapacity;
        this.evictUntil = evictUntil;
        this.loader = loader;
        this.map = new ConcurrentHashMap<>(maximumCapacity + Runtime.getRuntime().availableProcessors());
        this.queue = new ConcurrentLinkedQueue<>();
    }

    public V get(final K key) {
        final @Nullable V cached = this.map.get(key);
        if (cached != null) {
            return cached;
        }
        return this.loadValue(key);
    }

    private V loadValue(final K key) {
        final V load = this.loader.apply(key);
        final @Nullable V prevValue = this.map.putIfAbsent(key, load);
        if (prevValue != null) {
            // lost race to load entry
            return prevValue;
        }
        this.queue.offer(key);
        this.maybeEvictEntries();
        return load;
    }

    private void maybeEvictEntries() {
        if (this.map.size() > this.maximumCapacity) {
            while (this.map.size() > this.evictUntil) {
                final @Nullable K remove = this.queue.poll();
                if (remove == null) {
                    break;
                }
                this.map.remove(remove);
            }
        }
    }
}
