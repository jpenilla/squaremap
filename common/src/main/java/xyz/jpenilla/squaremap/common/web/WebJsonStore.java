package xyz.jpenilla.squaremap.common.web;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapDirectories;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.util.FileUtil;

@DefaultQualifier(NonNull.class)
@Singleton
public final class WebJsonStore {
    private static final int GZIP_MIN_LENGTH = 1024;

    public record Document(Path file, String data, byte @Nullable [] gzipped, long timestamp) {
        private static Document create(final Path file, final String data, final long timestamp) {
            // Only the embedded web server can serve the gzipped copy.
            final byte @Nullable [] gzipped = Config.HTTPD_ENABLED && data.length() >= GZIP_MIN_LENGTH ? gzip(data) : null;
            return new Document(file, data, gzipped, timestamp);
        }

        private static byte[] gzip(final String data) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length() / 2);
            try (final GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(data.getBytes(StandardCharsets.UTF_8));
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
            return out.toByteArray();
        }
    }

    private final SquaremapDirectories directories;
    private final Map<String, Document> cache = new ConcurrentHashMap<>();
    private final AtomicLong lastTimestamp = new AtomicLong();

    @Inject
    private WebJsonStore(final SquaremapDirectories directories) {
        this.directories = directories;
    }

    public @Nullable Document get(final String path) {
        return this.cache.get(path);
    }

    public void put(final Path file, final @Nullable String json) {
        this.cache.compute(this.requestPath(file), (path, existing) -> {
            if (json == null) {
                return null;
            }
            if (existing != null && existing.data.equals(json)) {
                return existing;
            }
            final Document entry = Document.create(file, json, this.nextTimestamp());
            if (Config.FLUSH_JSON_IMMEDIATELY || !Config.HTTPD_ENABLED) {
                write(file, json);
            }
            return entry;
        });
    }

    public void flush() {
        this.cache.values().forEach(entry -> write(entry.file, entry.data));
    }

    public void clear() {
        this.cache.clear();
    }

    /**
     * Forgets documents under a directory whose files were deleted, so the next put writes them again.
     *
     * @param directory a directory inside the web directory
     */
    public void clear(final Path directory) {
        final String prefix = this.requestPath(directory) + "/";
        this.cache.keySet().removeIf(path -> path.startsWith(prefix));
    }

    // Strictly increasing, so a document never repeats an earlier ETag, even one that was cleared.
    private long nextTimestamp() {
        final long now = System.currentTimeMillis();
        return this.lastTimestamp.updateAndGet(last -> Math.max(now, last + 1));
    }

    private String requestPath(final Path file) {
        final Path relative = this.directories.webDirectory().relativize(file);
        if (relative.isAbsolute() || relative.startsWith("..")) {
            throw new IllegalArgumentException(file + " is not inside the web directory");
        }
        return "/" + FileUtil.invariantSeparatorsPathString(relative);
    }

    private static void write(final Path file, final String data) {
        try {
            FileUtil.atomicWrite(file, tmp -> Files.writeString(tmp, data));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
