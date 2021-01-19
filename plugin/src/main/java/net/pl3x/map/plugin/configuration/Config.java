package net.pl3x.map.plugin.configuration;

import com.google.common.collect.ImmutableSet;
import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class Config {
    private static File CONFIG_FILE;
    static YamlConfiguration CONFIG;
    static int VERSION;

    public static void reload() {
        CONFIG_FILE = new File(Pl3xMapPlugin.getInstance().getDataFolder(), "config.yml");
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

    private static double getDouble(String path, double def) {
        CONFIG.addDefault(path, def);
        return CONFIG.getDouble(path, CONFIG.getDouble(path));
    }

    private static float getFloat(String path, float def) {
        CONFIG.addDefault(path, def);
        return (float) CONFIG.getDouble(path, CONFIG.getDouble(path));
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

    public static boolean COMPRESS_IMAGES = false;
    public static float COMPRESSION_RATIO = 0.0F;

    private static void imageQualitySettings() {
        COMPRESS_IMAGES = getBoolean("settings.image-quality.compress-images", COMPRESS_IMAGES);
        COMPRESSION_RATIO = getFloat("settings.image-quality.compress-images", COMPRESSION_RATIO);
    }

    public static boolean HTTPD_ENABLED = true;
    public static String HTTPD_BIND = "0.0.0.0";
    public static int HTTPD_PORT = 8080;

    private static void internalWebServerSettings() {
        HTTPD_ENABLED = getBoolean("settings.internal-webserver.enabled", HTTPD_ENABLED);
        HTTPD_BIND = getString("settings.internal-webserver.bind", HTTPD_BIND);
        HTTPD_PORT = getInt("settings.internal-webserver.port", HTTPD_PORT);
    }

    public static String UI_TITLE = "Pl3xMap - {world}";
    public static boolean UI_COORDINATES = true;
    public static boolean UI_LINK = true;
    public static String UI_SIDEBAR = "pinned";

    private static void uiSettings() {
        UI_TITLE = getString("settings.ui.title", UI_TITLE);
        UI_COORDINATES = getBoolean("settings.ui.coordinates", UI_COORDINATES);
        UI_LINK = getBoolean("settings.ui.link", UI_LINK);
        UI_SIDEBAR = getString("settings.ui.sidebar", UI_SIDEBAR);
    }

    private static final Map<Class<? extends Event>, Boolean> eventListenerToggles = new HashMap<>();

    public static boolean listenerEnabled(final @NonNull Class<? extends Event> eventClass) {
        final Boolean enabled = eventListenerToggles.get(eventClass);
        if (enabled == null) {
            Logger.warn(String.format("No configuration option found for event listener: %s, the listener will not be enabled.", eventClass.getSimpleName()));
            return false;
        }
        return enabled;
    }

    private static void listenerToggles() {
        ImmutableSet.of(
                BlockPlaceEvent.class,
                BlockBreakEvent.class,
                LeavesDecayEvent.class,
                BlockBurnEvent.class,
                BlockExplodeEvent.class,
                BlockGrowEvent.class,
                BlockFormEvent.class,
                EntityBlockFormEvent.class,
                BlockSpreadEvent.class,
                FluidLevelChangeEvent.class,
                EntityExplodeEvent.class,
                EntityChangeBlockEvent.class,
                StructureGrowEvent.class
        ).forEach(clazz -> eventListenerToggles.put(clazz, getBoolean("settings.event-listeners." + clazz.getSimpleName() + ".enabled", true)));

        ImmutableSet.of(
                BlockFromToEvent.class,
                PlayerJoinEvent.class,
                PlayerQuitEvent.class,
                PlayerMoveEvent.class,
                BlockPhysicsEvent.class,
                BlockPistonExtendEvent.class,
                BlockPistonRetractEvent.class,
                ChunkLoadEvent.class
        ).forEach(clazz -> eventListenerToggles.put(clazz, getBoolean("settings.event-listeners." + clazz.getSimpleName() + ".enabled", false)));
    }

    public static boolean CHUNK_LOAD_EVENT_ONLY_NEW_CHUNKS = true;

    private static void specialEventSettings() {
        CHUNK_LOAD_EVENT_ONLY_NEW_CHUNKS = getBoolean("settings.event-listeners.ChunkLoadEvent.only-new-chunks", true);
    }
}
