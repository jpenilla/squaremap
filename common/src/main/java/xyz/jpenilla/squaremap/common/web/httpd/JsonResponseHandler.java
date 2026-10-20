package xyz.jpenilla.squaremap.common.web.httpd;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.QValueParser;
import java.nio.ByteBuffer;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.web.WebJsonStore;

@DefaultQualifier(NonNull.class)
final class JsonResponseHandler {
    private final WebJsonStore jsonStore;

    JsonResponseHandler(final WebJsonStore jsonStore) {
        this.jsonStore = jsonStore;
    }

    boolean handle(final HttpServerExchange exchange) {
        final WebJsonStore.@Nullable Document cached = this.jsonStore.get(exchange.getRelativePath());
        if (cached == null) {
            return false;
        }

        final byte @Nullable [] gzipped = cached.gzipped();
        final boolean sendGzipped = gzipped != null && acceptsGzip(exchange);
        final String etag = sendGzipped ? cached.timestamp() + "-gz" : String.valueOf(cached.timestamp());
        exchange.getResponseHeaders().put(
            Headers.CONTENT_TYPE,
            "application/json"
        );
        exchange.getResponseHeaders().put(
            Headers.ETAG,
            etag
        );
        exchange.getResponseHeaders().put(
            Headers.CACHE_CONTROL,
            "no-cache"
        );
        if (gzipped != null) {
            exchange.getResponseHeaders().put(
                Headers.VARY,
                Headers.ACCEPT_ENCODING_STRING
            );
        }

        final String requestedEtag = exchange.getRequestHeaders().getFirst(Headers.IF_NONE_MATCH);
        if (requestedEtag != null && requestedEtag.equals(etag)) {
            exchange.setStatusCode(304);
            exchange.endExchange();
            return true;
        }

        if (sendGzipped) {
            exchange.getResponseHeaders().put(
                Headers.CONTENT_ENCODING,
                "gzip"
            );
            exchange.getResponseSender().send(ByteBuffer.wrap(gzipped));
        } else {
            exchange.getResponseSender().send(cached.data());
        }

        return true;
    }

    private static boolean acceptsGzip(final HttpServerExchange exchange) {
        final @Nullable HeaderValues values = exchange.getRequestHeaders().get(Headers.ACCEPT_ENCODING);
        if (values == null) {
            return false;
        }
        boolean wildcard = false;
        for (final List<QValueParser.QValueResult> group : QValueParser.parse(values)) {
            for (final QValueParser.QValueResult encoding : group) {
                if (encoding.getValue().equalsIgnoreCase("gzip")) {
                    return !encoding.isQValueZero();
                }
                if (encoding.getValue().equals("*")) {
                    wildcard = !encoding.isQValueZero();
                }
            }
        }
        return wildcard;
    }
}
