package net.pl3x.map.plugin.util;

import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class IntegratedServer {
    private static final String VERSION = "1.0";
    private static final String NAME = "Pl3xMapServer";
    private static final String FILENAME = NAME + "-" + VERSION + ".jar";
    private static final String URL = "https://repo.pl3x.net/net/pl3x/map/" + NAME + "/" + VERSION + "/" + FILENAME;

    public static boolean setup() {
        if (Bukkit.getPluginManager().isPluginEnabled(NAME)) {
            return true;
        }
        Path libsDir = FileUtil.PLUGIN_DIR.resolve("libs");
        try {
            Files.createDirectories(libsDir);
        } catch (IOException e) {
            Logger.severe(Lang.LOG_COULD_NOT_CREATE_DIR
                    .replace("{path}", libsDir.toAbsolutePath().toString()));
            return false;
        }
        File file = new File(libsDir.toString(), FILENAME);
        if (new JarDownloader().downloadJar(URL, file)) {
            if (Bukkit.getPluginManager().isPluginEnabled(NAME)) {
                return true;
            }
            try {
                Plugin plugin = Bukkit.getPluginManager().loadPlugin(file);
                if (plugin == null) {
                    return false;
                }
                Bukkit.getPluginManager().enablePlugin(plugin);
                return true;
            } catch (InvalidPluginException | InvalidDescriptionException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void startServer() {
        if (Config.HTTPD_ENABLED) {
            if (net.pl3x.map.Pl3xMapServer.startServer(Config.HTTPD_BIND, Config.HTTPD_PORT, FileUtil.WEB_DIR.toFile())) {
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
