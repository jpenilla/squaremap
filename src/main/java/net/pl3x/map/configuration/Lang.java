package net.pl3x.map.configuration;

import net.pl3x.map.Logger;
import net.pl3x.map.util.FileUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class Lang {
    public static String UNKNOWN_SUBCOMMAND = "&cUnknown subcommand";

    public static String WORLD_NOT_SPECIFIED = "&cMust specify a world";
    public static String WORLD_NOT_FOUND = "&cWorld not found";

    public static String RENDER_IN_PROGRESS = "&cA render is already in progress on {world}";
    public static String RENDER_NOT_IN_PROGRESS = "&cNo renders running for {world}";
    public static String FULL_RENDER_STARTED = "&aFull render started on {world}";
    public static String CANCELLED_RENDER = "&aRender cancelled for {world}";

    public static String INVALID_COMMAND_SYNTAX = "&cInvalid command syntax. Correct command syntax: &7/{syntax}";
    public static String NO_SUCH_WORLD = "&cNo such world '{world}'";
    public static String MAP_NOT_ENABLED_FOR_WORLD = "&cMap is not enabled for world '{world}'";

    public static String PLUGIN_RELOADED = "&a{name} v{version} reloaded";

    public static String PLUGIN_VERSION = "&a{name} v{version}";

    public static String LOG_COULD_NOT_CREATE_DIR = "Could not create directory! {path}";
    public static String LOG_COULD_NOT_SAVE_REGION = "Could not save map for region {x},{z}";
    public static String LOG_UNABLE_TO_WRITE_TO_FILE = "Unable to write to {path}";
    public static String LOG_STARTED_FULLRENDER = "&3Started full map render for &e{world}";
    public static String LOG_STARTED_RADIUSRENDER = "&3Started radius map render for &e{world}";
    public static String LOG_SCANNING_REGION_FILES = "&eScanning region files...";
    public static String LOG_FOUND_TOTAL_REGION_FILES = "&aFound &7{total} &aregion files";
    public static String LOG_FINISHED_RENDERING = "&3Finished rendering map for &e{world}";
    public static String LOG_RENDER_PROGRESS = "Render progress for {world}: {current_chunks}/{total_chunks} chunks ({percent}), Elapsed: {elapsed}, ETA: {eta}, Rate: {rate}cps";
    public static String LOG_SCANNING_REGIONS_FINISHED = "&eWorld&3: &7{world} &eProcessed&3: &7{chunks} chunks &e(&3{percent}&e)&7";
    public static String LOG_SAVING_CHUNKS_FOR_REGION = "        &aSaving {total} chunks for region {x},{z}";
    public static String LOG_SKIPPING_EMPTY_REGION = "        &cRegion is empty. Skipping. {x},{z}";

    public static String LOG_INTERNAL_WEB_DISABLED = "Internal webserver is disabled in config.yml";
    public static String LOG_INTERNAL_WEB_STARTED = "&aInternal webserver running on {bind}:{port}";
    public static String LOG_INTERNAL_WEB_START_ERROR = "Internal webserver could not start";
    public static String LOG_INTERNAL_WEB_STOPPED = "&aInternal webserver stopped";
    public static String LOG_INTERNAL_WEB_STOP_ERROR = "Internal webserver is not running";

    public static String LOG_JARLOADER_DOWNLOADING = "&eDownloading integrated server jar from &7{url}";
    public static String LOG_JARLOADER_PROGRESS = "&3{progress} &7(&3{current}&e/&3{total}&7)";

    private static void init() {
        UNKNOWN_SUBCOMMAND = getString("unknown-subcommand", UNKNOWN_SUBCOMMAND);

        WORLD_NOT_SPECIFIED = getString("world-not-specified", WORLD_NOT_SPECIFIED);
        WORLD_NOT_FOUND = getString("world-not-found", WORLD_NOT_FOUND);

        RENDER_IN_PROGRESS = getString("render-in-progress", RENDER_IN_PROGRESS);
        RENDER_NOT_IN_PROGRESS = getString("render-not-in-progress", RENDER_NOT_IN_PROGRESS);
        FULL_RENDER_STARTED = getString("full-render-started", FULL_RENDER_STARTED);
        CANCELLED_RENDER = getString("cancelled-render", CANCELLED_RENDER);

        INVALID_COMMAND_SYNTAX = getString("invalid-command-syntax", INVALID_COMMAND_SYNTAX);
        NO_SUCH_WORLD = getString("no-such-world", NO_SUCH_WORLD);
        MAP_NOT_ENABLED_FOR_WORLD = getString("map-not-enabled-for-world", MAP_NOT_ENABLED_FOR_WORLD);

        PLUGIN_RELOADED = getString("plugin-reloaded", PLUGIN_RELOADED);
        PLUGIN_VERSION = getString("plugin-version", PLUGIN_VERSION);

        LOG_COULD_NOT_CREATE_DIR = getString("log.could-not-create-directory", LOG_COULD_NOT_CREATE_DIR);
        LOG_COULD_NOT_SAVE_REGION = getString("log.could-not-save-region", LOG_COULD_NOT_SAVE_REGION);
        LOG_UNABLE_TO_WRITE_TO_FILE = getString("log.unable-to-write-to-file", LOG_UNABLE_TO_WRITE_TO_FILE);
        LOG_STARTED_FULLRENDER = getString("log.started-full-render", LOG_STARTED_FULLRENDER);
        LOG_SCANNING_REGION_FILES = getString("log.scanning-region-files", LOG_SCANNING_REGION_FILES);
        LOG_FOUND_TOTAL_REGION_FILES = getString("log.found-total-region-files", LOG_FOUND_TOTAL_REGION_FILES);
        LOG_FINISHED_RENDERING = getString("log.finished-rendering", LOG_FINISHED_RENDERING);
        LOG_RENDER_PROGRESS = getString("log.scanning-region-progress", LOG_RENDER_PROGRESS);
        LOG_SCANNING_REGIONS_FINISHED = getString("log.scanning-region-finished", LOG_SCANNING_REGIONS_FINISHED);
        LOG_SAVING_CHUNKS_FOR_REGION = getString("log.saving-chunks-for-region", LOG_SAVING_CHUNKS_FOR_REGION);
        LOG_SKIPPING_EMPTY_REGION = getString("log.skipping-empty-region", LOG_SKIPPING_EMPTY_REGION);

        LOG_INTERNAL_WEB_DISABLED = getString("log.internal-web-disabled", LOG_INTERNAL_WEB_DISABLED);
        LOG_INTERNAL_WEB_STARTED = getString("log.internal-web-started", LOG_INTERNAL_WEB_STARTED);
        LOG_INTERNAL_WEB_START_ERROR = getString("log.internal-web-start-error", LOG_INTERNAL_WEB_START_ERROR);
        LOG_INTERNAL_WEB_STOPPED = getString("log.internal-web-stopped", LOG_INTERNAL_WEB_STOPPED);
        LOG_INTERNAL_WEB_STOP_ERROR = getString("log.internal-web-not-running", LOG_INTERNAL_WEB_STOP_ERROR);

        LOG_JARLOADER_DOWNLOADING = getString("log.dependency-downloading", LOG_JARLOADER_DOWNLOADING);
        LOG_JARLOADER_PROGRESS = getString("log.dependency-progress", LOG_JARLOADER_PROGRESS);
    }

    public static void reload() {
        File configFile = FileUtil.PLUGIN_DIR.resolve(Config.LANGUAGE_FILE).toFile();
        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException ignore) {
        } catch (InvalidConfigurationException ex) {
            Logger.log().log(Level.SEVERE, "Could not load " + Config.LANGUAGE_FILE + ", please correct your syntax errors", ex);
            throw new RuntimeException(ex);
        }
        config.options().copyDefaults(true);

        Lang.init();

        try {
            config.save(configFile);
        } catch (IOException ex) {
            Logger.log().log(Level.SEVERE, "Could not save " + configFile, ex);
        }
    }

    private static YamlConfiguration config;

    private static String getString(String path, String def) {
        config.addDefault(path, def);
        return colorize(config.getString(path, config.getString(path)));
    }

    public static void send(CommandSender recipient, String message) {
        if (recipient != null) {
            for (String part : split(message)) {
                recipient.sendMessage(part);
            }
        }
    }

    public static String colorize(String str) {
        if (str == null) {
            return "";
        }
        str = ChatColor.translateAlternateColorCodes('&', str);
        if (ChatColor.stripColor(str).isEmpty()) {
            return "";
        }
        return str;
    }

    public static String[] split(String msg) {
        return msg.split("\n");
    }
}
