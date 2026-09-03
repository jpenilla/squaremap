package xyz.jpenilla.squaremap.common.data;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.httpd.JsonCache;
import xyz.jpenilla.squaremap.common.util.Util;

/**
 * Tracks which tile images have recently been rewritten and publishes them to a manifest
 * in the world tiles directory. The web interface polls the manifest so that it only
 * refetches tiles that actually changed, instead of periodically discarding every tile in
 * the viewport.
 */
@DefaultQualifier(NonNull.class)
public final class TileUpdates {
    public static final String FILE_NAME = "updates.json";
    private static final int MAX_ENTRIES = 2048;
    private static final long MIN_WRITE_INTERVAL_MS = 500L;

    private final String jsonPathString;
    private final JsonCache jsonCache;
    private final Map<String, Long> tiles = new LinkedHashMap<>();
    private long newestDropped = 0L;
    private boolean dirty = false;
    private long lastWrite = 0L;

    public TileUpdates(final DirectoryProvider directoryProvider, final Path tilesPath, final JsonCache jsonCache) {
        final Path jsonPath = tilesPath.resolve(FILE_NAME);
        this.jsonPathString = "/" + directoryProvider.webDirectory().relativize(jsonPath).toString().replace("\\", "/");
        this.jsonCache = jsonCache;
        // publish an empty manifest so clients don't read leftover state from a previous run
        this.write();
    }

    /**
     * Records that the given tile image has been rewritten.
     *
     * @param tile tile written to disk
     */
    public synchronized void record(final TileCoordinate tile) {
        final String key = tile.key();
        // remove before putting so the map stays ordered from oldest to newest
        this.tiles.remove(key);
        this.tiles.put(key, System.currentTimeMillis());

        final Iterator<Map.Entry<String, Long>> it = this.tiles.entrySet().iterator();
        while (this.tiles.size() > MAX_ENTRIES && it.hasNext()) {
            this.newestDropped = Math.max(this.newestDropped, it.next().getValue());
            it.remove();
        }

        this.dirty = true;
    }

    /**
     * Publishes the manifest if it has changed and the minimum write interval has elapsed.
     */
    public synchronized void writeIfDue() {
        if (this.dirty && System.currentTimeMillis() - this.lastWrite >= MIN_WRITE_INTERVAL_MS) {
            this.write();
        }
    }

    /**
     * Publishes the manifest if it has changed, ignoring the minimum write interval.
     */
    public synchronized void writeIfDirty() {
        if (this.dirty) {
            this.write();
        }
    }

    private void write() {
        final long now = System.currentTimeMillis();

        final Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("timestamp", now);
        // clients that last saw a manifest at or before this time may have missed an update
        manifest.put("dropped", this.newestDropped);
        manifest.put("tiles", this.tiles);

        this.jsonCache.put(this.jsonPathString, Util.gson().toJson(manifest));
        this.dirty = false;
        this.lastWrite = now;
    }
}
