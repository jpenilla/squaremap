package net.pl3x.map.configuration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.pl3x.map.Logger;
import net.pl3x.map.util.FileUtil;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

@SuppressWarnings("unused")
public class Config {
    private static File CONFIG_FILE;
    static YamlConfiguration CONFIG;
    static int VERSION;

    public static void reload() {
        CONFIG_FILE = new File(FileUtil.getPluginFolder(), "config.yml");
        CONFIG = new YamlConfiguration();
        try {
            CONFIG.load(CONFIG_FILE);
        } catch (IOException ignore) {
        } catch (InvalidConfigurationException ex) {
            Logger.severe("Could not load config.yml, please correct your syntax errors");
            throw new RuntimeException(ex);
        }
        CONFIG.options().copyDefaults(true);

        VERSION = getInt("config-version", 1);
        set("config-version", 1);

        readConfig(Config.class, null);

        WorldConfig.reload();
    }

    static void readConfig(Class<?> clazz, Object instance) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPrivate(method.getModifiers())) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE) {
                    try {
                        method.setAccessible(true);
                        method.invoke(instance);
                    } catch (InvocationTargetException ex) {
                        throw new RuntimeException(ex.getCause());
                    } catch (Exception ex) {
                        Logger.severe("Error invoking " + method);
                        ex.printStackTrace();
                    }
                }
            }
        }

        try {
            CONFIG.save(CONFIG_FILE);
        } catch (IOException ex) {
            Logger.severe("Could not save " + CONFIG_FILE);
            ex.printStackTrace();
        }
    }

    private static void set(String path, Object val) {
        CONFIG.addDefault(path, val);
        CONFIG.set(path, val);
    }

    private static String getString(String path, String def) {
        CONFIG.addDefault(path, def);
        return CONFIG.getString(path, CONFIG.getString(path));
    }

    private static boolean getBoolean(String path, boolean def) {
        CONFIG.addDefault(path, def);
        return CONFIG.getBoolean(path, CONFIG.getBoolean(path));
    }

    private static int getInt(String path, int def) {
        CONFIG.addDefault(path, def);
        return CONFIG.getInt(path, CONFIG.getInt(path));
    }

    public static String LANGUAGE_FILE = "lang-en.yml";
    public static boolean DEBUG_MODE = false;

    private static void baseSettings() {
        LANGUAGE_FILE = getString("settings.language-file", LANGUAGE_FILE);
        DEBUG_MODE = getBoolean("settings.debug-mode", DEBUG_MODE);
    }

    public static String WEB_DIR = "web";
    public static boolean UPDATE_WEB_DIR = true;

    private static void webDirSettings() {
        WEB_DIR = getString("settings.web-directory.path", WEB_DIR);
        UPDATE_WEB_DIR = getBoolean("settings.web-directory.auto-update", UPDATE_WEB_DIR);
    }

    public static boolean HTTPD_ENABLED = true;
    public static String HTTPD_BIND = "0.0.0.0";
    public static int HTTPD_PORT = 8080;

    private static void internalWebServerSettings() {
        HTTPD_ENABLED = getBoolean("settings.internal-webserver.enabled", HTTPD_ENABLED);
        HTTPD_BIND = getString("settings.internal-webserver.bind", HTTPD_BIND);
        HTTPD_PORT = getInt("settings.internal-webserver.port", HTTPD_PORT);
    }
}
