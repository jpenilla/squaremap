package xyz.jpenilla.squaremap.common.httpd;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.FileUtil;

@DefaultQualifier(NonNull.class)
@Singleton
public final class JsonCache {
    private final DirectoryProvider directoryProvider;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Inject
    private JsonCache(final DirectoryProvider directoryProvider) {
        this.directoryProvider = directoryProvider;
    }

    boolean handle(final HttpServerExchange exchange) {
        final @Nullable String cached = this.cache.get(exchange.getRelativePath());
        if (cached == null) {
            return false;
        }

        exchange.getRequestHeaders().put(
            Headers.CONTENT_TYPE,
            "application/json"
        );

        exchange.getResponseSender().send(cached);

        return true;
    }

    public void put(final String path, final @Nullable String json) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException(path);
        }

        if (json == null) {
            this.cache.remove(path);
            return;
        }

        this.cache.put(path, json);

        if (Config.FLUSH_JSON_IMMEDIATELY || !Config.HTTPD_ENABLED) {
            this.write(path, json);
        }
    }

    public void flush() {
        this.cache.forEach(this::write);
    }

    public void clear() {
        this.cache.clear();
    }

    private void write(final String path, final String data) {
        try {
            FileUtil.atomicWrite(this.directoryProvider.webDirectory().resolve("." + path), tmp -> Files.writeString(tmp, data));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
