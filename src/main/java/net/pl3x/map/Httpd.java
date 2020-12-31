package net.pl3x.map;

import fi.iki.elonen.NanoHTTPD;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.util.FileUtil;

public class Httpd extends NanoHTTPD {
    private static Httpd httpd;
    private final File webDir;

    public Httpd() {
        super(Config.HTTPD_PORT);
        this.webDir = FileUtil.getWebFolder();
    }

    public static void startServer() {
        if (Config.HTTPD_ENABLED) {
            try {
                httpd = new Httpd();
                httpd.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                Logger.info(Lang.LOG_INTERNAL_WEB_STARTED
                        .replace("{port}", Integer.toString(Config.HTTPD_PORT)));
            } catch (IOException e) {
                Logger.severe(Lang.LOG_INTERNAL_WEB_START_ERROR);
                e.printStackTrace();
            }
        } else {
            Logger.warn(Lang.LOG_INTERNAL_WEB_DISABLED);
        }
    }

    public static void stopServer() {
        if (httpd != null) {
            httpd.stop();
            httpd = null;
            Logger.info(Lang.LOG_INTERNAL_WEB_STOPPED);
        } else {
            Logger.warn(Lang.LOG_INTERNAL_WEB_STOP_ERROR);
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if (uri.startsWith("..") || uri.endsWith("..") || uri.contains("../")) {
            return newFixedLengthResponse("403");
        }
        if (uri.isEmpty() || uri.equals("/")) {
            uri = "/index.html";
        }
        File file = new File(webDir.getAbsolutePath() + uri);
        if (!file.exists()) {
            return newFixedLengthResponse("404");
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            String mime = NanoHTTPD.getMimeTypeForFile(file.getName());
            return newFixedLengthResponse(Response.Status.OK, mime, fis, file.length());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return newFixedLengthResponse("403");
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse("500");
        }
    }
}
