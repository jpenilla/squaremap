package net.pl3x.map.plugin.httpd;

import io.undertow.Undertow;
import io.undertow.UndertowLogger;
import io.undertow.UndertowOptions;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.ETag;
import io.undertow.util.Headers;
import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

public class IntegratedServer {
    private static Undertow server;

    public static void startServer() {
        try {
            ResourceHandler resourceHandler = new ResourceHandler(PathResourceManager.builder()
                    .setBase(Paths.get(FileUtil.WEB_DIR.toFile().getAbsolutePath()))
                    .setETagFunction((path) -> {
                        try {
                            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                            long time = attr.lastModifiedTime().toMillis();
                            return new ETag(false, Long.toString(time));
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .build(), exchange -> {
                String url = exchange.getRelativePath();
                if (url.startsWith("/tiles") && url.endsWith(".png")) {
                    exchange.setStatusCode(200);
                    return;
                }
                exchange.setStatusCode(404);
                if (UndertowLogger.PREDICATE_LOGGER.isDebugEnabled()) {
                    UndertowLogger.PREDICATE_LOGGER.debugf("Response code set to [%s] for %s.", 404, exchange);
                }
            });

            server = Undertow.builder()
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .addHttpListener(Config.HTTPD_PORT, Config.HTTPD_BIND)
                    .setHandler(exchange -> {
                        if (exchange.getRelativePath().startsWith("/tiles")) {
                            exchange.getResponseHeaders().put(Headers.CACHE_CONTROL,
                                                              "max-age=0, must-revalidate, no-cache");
                        }
                        resourceHandler.handleRequest(exchange);
                    })
                    .build();
            server.start();

            Logger.info(
                    Lang.LOG_INTERNAL_WEB_STARTED,
                    Template.of("bind", Config.HTTPD_BIND),
                    Template.of("port", Integer.toString(Config.HTTPD_PORT))
            );
        } catch (Exception e) {
            server = null;
            Logger.severe(Lang.LOG_INTERNAL_WEB_START_ERROR, e);
        }
    }

    public static void stopServer() {
        if (server == null) {
            Logger.warn(Lang.LOG_INTERNAL_WEB_STOP_ERROR);
            return;
        }

        server.stop();
        server = null;
        Logger.info(Lang.LOG_INTERNAL_WEB_STOPPED);
    }
}
