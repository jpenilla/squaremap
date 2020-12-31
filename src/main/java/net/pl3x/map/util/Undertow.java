package net.pl3x.map.util;

import java.io.File;
import net.pl3x.map.Logger;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.hook.undertow.UndertowHook;

public class Undertow {
    public static boolean setup() {
        return new JarLoader().loadJar("https://purpur.pl3x.net/UndertowHook-2.2.3.jar",
                new File(FileUtil.getLibsFolder(), "UndertowHook-2.2.3.jar"));
    }

    public static void startServer() {
        if (Config.HTTPD_ENABLED) {
            UndertowHook.startServer("localhost", Config.HTTPD_PORT, FileUtil.getWebFolder(), 100);
        } else {
            Logger.warn(Lang.LOG_INTERNAL_WEB_DISABLED);
        }
    }

    public static void stopServer() {
        if (Config.HTTPD_ENABLED) {
            UndertowHook.stopServer();
        } else {
            Logger.warn(Lang.LOG_INTERNAL_WEB_DISABLED);
        }
    }
}
