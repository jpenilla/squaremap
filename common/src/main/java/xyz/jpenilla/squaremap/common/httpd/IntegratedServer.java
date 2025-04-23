package xyz.jpenilla.squaremap.common.httpd;

import io.undertow.Undertow;
import io.undertow.UndertowLogger;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.ETag;
import io.undertow.util.Headers;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.Util;

public final class IntegratedServer {
    private static final boolean DEV_FRONTEND = Boolean.getBoolean("squaremap.devFrontend");
    private static final String FRONTEND_PATH = System.getProperty("squaremap.frontendPath");
    private static ViteRunner VITE_RUNNER;
    private static Undertow SERVER;
    private static JsonCache CACHE;

    private IntegratedServer() {
    }

    public static void startServer(final DirectoryProvider directoryProvider, final JsonCache jsonCache) {
        if (DEV_FRONTEND && FRONTEND_PATH != null) {
            VITE_RUNNER = new ViteRunner(FRONTEND_PATH);
            VITE_RUNNER.start();
        }

        CACHE = jsonCache;
        try {
            SERVER = buildUndertow(createResourceHandler(directoryProvider));
            SERVER.start();

            Logging.info(Messages.LOG_INTERNAL_WEB_STARTED, "bind", Config.HTTPD_BIND, "port", Config.HTTPD_PORT);
        } catch (Exception e) {
            SERVER = null;
            Logging.logger().error(Messages.LOG_INTERNAL_WEB_START_ERROR, e);
        }
    }

    private static Undertow buildUndertow(final ResourceHandler resourceHandler) {
        return Undertow.builder()
            .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
            .addHttpListener(Config.HTTPD_PORT, Config.HTTPD_BIND)
            .setHandler(createHttpHandler(resourceHandler))
            .build();
    }

    private static String getViteUrl() {
        try {
            return VITE_RUNNER.getUrl().get(15, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            Logging.logger().error("Failed to get Vite URL", e);
            VITE_RUNNER.shutdown();
        }
        return null;
    }

    private static HttpHandler createHttpHandler(final ResourceHandler resourceHandler) {
        ProxyHandler devProxyHandler = null;
        if (VITE_RUNNER != null) {
            final String viteUrl = getViteUrl();
            if (viteUrl != null) {
                final URI viteUri;
                try {
                    viteUri = new URI(viteUrl);
                } catch (final URISyntaxException e) {
                    throw Util.rethrow(e);
                }
                final ProxyClient proxyClient = new LoadBalancingProxyClient()
                    .addHost(viteUri);
                devProxyHandler = ProxyHandler.builder()
                    .setProxyClient(proxyClient)
                    .build();
            }
        }
        final ProxyHandler finalProxyHandler = devProxyHandler;

        return exchange -> {
            if (CACHE.handle(exchange)) {
                return;
            }

            if (exchange.getRelativePath().startsWith("/tiles")) {
                exchange.getResponseHeaders().put(
                    Headers.CACHE_CONTROL,
                    "max-age=0, must-revalidate, no-cache"
                );
            }

            if (finalProxyHandler != null
                && !exchange.getRelativePath().startsWith("/tiles")
                && !exchange.getRelativePath().startsWith("/images/icon/registered")) {
                finalProxyHandler.handleRequest(exchange);
            } else {
                resourceHandler.handleRequest(exchange);
            }
        };
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
        if (SERVER == null) {
            Logging.logger().warn(Messages.LOG_INTERNAL_WEB_STOP_ERROR);
            return;
        }

        SERVER.stop();
        SERVER = null;
        if (VITE_RUNNER != null) {
            VITE_RUNNER.shutdown();
            VITE_RUNNER = null;
        }
        Logging.logger().info(Messages.LOG_INTERNAL_WEB_STOPPED);
    }
}
