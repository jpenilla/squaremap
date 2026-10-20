package xyz.jpenilla.squaremap.common.web;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.coordinate.TileCoordinate;

/**
 * Tracks recently written tiles so web clients can reload only the tiles that changed.
 */
@DefaultQualifier(NonNull.class)
public final class TileChangeLog {
    public static final String FILE_NAME = "changes.json";
    private static final int MAX_ENTRIES = 4096;
    private static final long PUBLISH_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(500);

    private final String epoch = Long.toString(ThreadLocalRandom.current().nextLong() >>> 1, Character.MAX_RADIX);
    private final Consumer<String> publisher;
    private final int maxEntries;
    private final LongFunction<Executor> delayedExecutor;
    // Held while publishing so nothing is published after close returns.
    private final Object publishLock = new Object();
    private final LinkedHashMap<TileCoordinate, Long> changes = new LinkedHashMap<>();
    private long seq;
    private long dropped;
    private boolean publishScheduled;
    private long lastPublish;
    private boolean closed;

    TileChangeLog(final Consumer<String> publisher, final int maxEntries, final LongFunction<Executor> delayedExecutor) {
        this.publisher = publisher;
        this.maxEntries = maxEntries;
        this.delayedExecutor = delayedExecutor;
        this.publish();
    }

    public synchronized void record(final TileCoordinate tile) {
        if (this.closed) {
            return;
        }
        this.changes.remove(tile);
        this.changes.put(tile, ++this.seq);
        if (this.changes.size() > this.maxEntries) {
            final Iterator<Long> eldest = this.changes.values().iterator();
            this.dropped = eldest.next();
            eldest.remove();
        }
        if (!this.publishScheduled) {
            this.publishScheduled = true;
            final long delay = Math.max(0, this.lastPublish + PUBLISH_INTERVAL_NANOS - System.nanoTime());
            this.delayedExecutor.apply(delay).execute(this::publish);
        }
    }

    void publish() {
        synchronized (this.publishLock) {
            final String json;
            synchronized (this) {
                if (this.closed) {
                    return;
                }
                this.publishScheduled = false;
                this.lastPublish = System.nanoTime();
                json = this.json();
            }
            this.accept(json);
        }
    }

    public void close() {
        synchronized (this.publishLock) {
            final String json;
            synchronized (this) {
                if (this.closed) {
                    return;
                }
                this.closed = true;
                json = this.json();
            }
            // Publish changes still waiting for a scheduled publish.
            this.accept(json);
        }
    }

    private void accept(final String json) {
        try {
            this.publisher.accept(json);
        } catch (final RuntimeException ex) {
            Logging.logger().warn("Failed to publish tile changes", ex);
        }
    }

    private String json() {
        final StringBuilder json = new StringBuilder(64 + this.changes.size() * 24)
            .append("{\"epoch\":\"").append(this.epoch)
            .append("\",\"seq\":").append(this.seq)
            .append(",\"dropped\":").append(this.dropped)
            .append(",\"changes\":[");
        boolean first = true;
        for (final Map.Entry<TileCoordinate, Long> change : this.changes.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            final TileCoordinate tile = change.getKey();
            json.append('[').append(change.getValue())
                .append(',').append(tile.zoom())
                .append(',').append(tile.x())
                .append(',').append(tile.z())
                .append(']');
        }
        return json.append("]}").toString();
    }

    public static TileChangeLog create(final WebJsonStore store, final Path file) {
        return new TileChangeLog(json -> store.put(file, json), MAX_ENTRIES, delay -> CompletableFuture.delayedExecutor(delay, TimeUnit.NANOSECONDS));
    }
}
