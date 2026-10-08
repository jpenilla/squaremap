package xyz.jpenilla.squaremap.common.web;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapDirectories;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.util.FileUtil;

@DefaultQualifier(NonNull.class)
@Singleton
public final class WebJsonStore {
    public record Document(String data, long timestamp) {
        private static Document create(final String data) {
            return new Document(data, System.currentTimeMillis());
        }
    }

    private final SquaremapDirectories directories;
    private final Map<String, Document> cache = new ConcurrentHashMap<>();

    @Inject
    private WebJsonStore(final SquaremapDirectories directories) {
        this.directories = directories;
    }

    public @Nullable Document get(final String path) {
        return this.cache.get(path);
    }

    public void put(final String path, final @Nullable String json) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException(path);
        }
        this.cache.compute(path, (pathKey, existing) -> {
            if (json == null) {
                return null;
            }
            if (existing != null && existing.data.equals(json)) {
                return existing;
            }
            final Document entry = Document.create(json);
            if (Config.FLUSH_JSON_IMMEDIATELY || !Config.HTTPD_ENABLED) {
                this.write(path, json);
            }
            return entry;
        });
    }

    public void flush() {
        this.cache.forEach((path, entry) -> this.write(path, entry.data));
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
        final String prefix = "/" + this.directories.webDirectory().relativize(directory).toString().replace("\\", "/") + "/";
        this.cache.keySet().removeIf(path -> path.startsWith(prefix));
    }

    private void write(final String path, final String data) {
        try {
            FileUtil.atomicWrite(this.directories.webDirectory().resolve("." + path), tmp -> Files.writeString(tmp, data));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
