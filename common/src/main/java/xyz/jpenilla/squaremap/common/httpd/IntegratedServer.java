package xyz.jpenilla.squaremap.common.httpd;

import io.undertow.Undertow;
import io.undertow.UndertowLogger;
import io.undertow.UndertowOptions;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.ETag;
import io.undertow.util.Headers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;

public class IntegratedServer {
    private static Undertow server;

    public static void startServer(final DirectoryProvider directoryProvider) {
        try {
            server = buildUndertow(createResourceHandler(directoryProvider));
            server.start();

            Logging.info(Lang.LOG_INTERNAL_WEB_STARTED, "bind", Config.HTTPD_BIND, "port", Config.HTTPD_PORT);
        } catch (Exception e) {
            server = null;
            Logging.logger().error(Lang.LOG_INTERNAL_WEB_START_ERROR, e);
        }
    }

    private static Undertow buildUndertow(final ResourceHandler resourceHandler) {
        return Undertow.builder()
            .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
            .addHttpListener(Config.HTTPD_PORT, Config.HTTPD_BIND)
            .setHandler(exchange -> {
                if (exchange.getRelativePath().startsWith("/tiles")) {
                    exchange.getResponseHeaders().put(
                        Headers.CACHE_CONTROL,
                        "max-age=0, must-revalidate, no-cache"
                    );
                }
                resourceHandler.handleRequest(exchange);
            })
            .build();
    }

    private static ResourceHandler createResourceHandler(final DirectoryProvider directoryProvider) {
        final ResourceManager resourceManager = PathResourceManager.builder()
            .setBase(Paths.get(directoryProvider.webDirectory().toFile().getAbsolutePath()))
            .setETagFunction((path) -> {
                final BasicFileAttributes attr;
                try {
                    attr = Files.readAttributes(path, BasicFileAttributes.class);
                } catch (final IOException e) {
                    Logging.logger().warn("Failed to read file attributes for {}", path, e);
                    return null;
                }
                long time = attr.lastModifiedTime().toMillis();
                return new ETag(false, Long.toString(time));
            })
            .build();

        return new ResourceHandler(
            resourceManager,
            exchange -> {
                final String url = exchange.getRelativePath();
                if (url.startsWith("/tiles") && url.endsWith(".png")) {
                    exchange.setStatusCode(200);
                    return;
                }
                exchange.setStatusCode(404);
                if (UndertowLogger.PREDICATE_LOGGER.isDebugEnabled()) {
                    UndertowLogger.PREDICATE_LOGGER.debugf("Response code set to [%s] for %s.", 404, exchange);
                }
            }
        );
    }

    public static void stopServer() {
        if (server == null) {
            Logging.logger().warn(Lang.LOG_INTERNAL_WEB_STOP_ERROR);
            return;
        }

        server.stop();
        server = null;
        Logging.logger().info(Lang.LOG_INTERNAL_WEB_STOPPED);
    }
}
