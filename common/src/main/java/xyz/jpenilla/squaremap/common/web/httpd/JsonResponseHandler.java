package xyz.jpenilla.squaremap.common.web.httpd;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
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

        final String timestamp = String.valueOf(cached.timestamp());
        exchange.getResponseHeaders().put(
            Headers.CONTENT_TYPE,
            "application/json"
        );
        exchange.getResponseHeaders().put(
            Headers.ETAG,
            timestamp
        );

        final String requestedEtag = exchange.getRequestHeaders().getFirst(Headers.IF_NONE_MATCH);
        if (requestedEtag != null && requestedEtag.equals(timestamp)) {
            exchange.setStatusCode(304);
            exchange.endExchange();
            return true;
        }

        exchange.getResponseSender().send(cached.data());

        return true;
    }
}
