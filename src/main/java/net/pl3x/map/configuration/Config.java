package net.pl3x.map.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import net.pl3x.map.Logger;
import net.pl3x.map.Pl3xMap;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class Config {
    public static String LANGUAGE_FILE = "lang-en.yml";
    public static boolean DEBUG_MODE = false;

    public static String WEB_DIR = "web";

    public static boolean HTTPD_ENABLED = true;
    public static int HTTPD_PORT = 8080;

    private static void init() {
        LANGUAGE_FILE = getString("language-file", LANGUAGE_FILE);
        DEBUG_MODE = getBoolean("debug-mode", DEBUG_MODE);

        WEB_DIR = getString("web-directory", WEB_DIR);
        File base = Pl3xMap.getInstance().getDataFolder();
        File test = new File(base, WEB_DIR);
        try {
            Path basePath = Path.of(base.getCanonicalPath()).normalize();
            Path testPath = Path.of(test.getCanonicalPath()).normalize();
            if (!testPath.startsWith(basePath)) {
                WEB_DIR = "web";
                Logger.severe("Directory traversal attack detected!");
                Logger.severe("Falling back to default \"web\" directory");
            }
        } catch (IOException e) {
            WEB_DIR = "web";
            Logger.severe("Configured web directory not found!");
            Logger.severe("Falling back to default \"web\" directory");
        }

        HTTPD_ENABLED = getBoolean("internal-webserver.enabled", HTTPD_ENABLED);
        HTTPD_PORT = getInt("internal-webserver.port", HTTPD_PORT);
    }

    public static void reload() {
        Pl3xMap plugin = Pl3xMap.getInstance();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException ignore) {
        } catch (InvalidConfigurationException ex) {
            Logger.log().log(Level.SEVERE, "Could not load config.yml, please correct your syntax errors", ex);
            throw new RuntimeException(ex);
        }
        config.options().header("This is the configuration file for " + plugin.getName());
        config.options().copyDefaults(true);

        Config.init();

        try {
            config.save(configFile);
        } catch (IOException ex) {
            Logger.log().log(Level.SEVERE, "Could not save " + configFile, ex);
        }
    }

    private static YamlConfiguration config;

    private static String getString(String path, String def) {
        config.addDefault(path, def);
        return config.getString(path, config.getString(path));
    }

    private static boolean getBoolean(String path, boolean def) {
        config.addDefault(path, def);
        return config.getBoolean(path, config.getBoolean(path));
    }

    private static int getInt(String path, int def) {
        config.addDefault(path, def);
        return config.getInt(path, config.getInt(path));
    }
}
