package net.pl3x.map.util;

import java.io.File;
import net.pl3x.map.Logger;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.configuration.Lang;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;

public class IntegratedServer {
    public static boolean setup() {
        if (Bukkit.getPluginManager().isPluginEnabled("Pl3xMapServer")) {
            return true;
        }
        File file = new File(FileUtil.getLibsFolder(), "Pl3xMapServer-1.0.jar");
        if (new JarDownloader().downloadJar("https://repo.pl3x.net/snapshots/net/pl3x/map/Pl3xMapServer/1.0/Pl3xMapServer-1.0.jar", file)) {
            try {
                return Bukkit.getPluginManager().loadPlugin(file) != null;
            } catch (InvalidPluginException | InvalidDescriptionException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void startServer() {
        if (Config.HTTPD_ENABLED) {
            if (net.pl3x.map.Pl3xMapServer.startServer(Config.HTTPD_BIND, Config.HTTPD_PORT, FileUtil.getWebFolder())) {
                Logger.info(Lang.LOG_INTERNAL_WEB_STARTED
                        .replace("{bind}", Config.HTTPD_BIND)
                        .replace("{port}", Integer.toString(Config.HTTPD_PORT)));
            } else {
                Logger.severe(Lang.LOG_INTERNAL_WEB_START_ERROR);
            }
        } else {
            Logger.warn(Lang.LOG_INTERNAL_WEB_DISABLED);
        }
    }

    public static void stopServer() {
        if (Config.HTTPD_ENABLED) {
            if (net.pl3x.map.Pl3xMapServer.stopServer()) {
                Logger.info(Lang.LOG_INTERNAL_WEB_STOPPED);
            } else {
                Logger.warn(Lang.LOG_INTERNAL_WEB_STOP_ERROR);
            }
        } else {
            Logger.warn(Lang.LOG_INTERNAL_WEB_DISABLED);
        }
    }
}
