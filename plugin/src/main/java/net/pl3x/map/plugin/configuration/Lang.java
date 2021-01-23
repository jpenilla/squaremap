package net.pl3x.map.plugin.configuration;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.util.FileUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class Lang {

    // MiniMessage formatted strings, to be sent using Logger.info(String, Template...) or send(CommandSender, Template...)
    public static String RENDER_IN_PROGRESS = "<red>A render is already in progress on <world>";
    public static String RENDER_NOT_IN_PROGRESS = "<red>No renders running for <world>";
    public static String CANCELLED_RENDER = "<green>Render cancelled for <world>";

    public static String CONSOLE_MUST_SPECIFY_WORLD = "<red>You must specify the world when running this command from console";
    public static String NO_SUCH_WORLD = "<red>No such world '<world>'";
    public static String MAP_NOT_ENABLED_FOR_WORLD = "<red>Map is not enabled for world '<world>'";
    public static String CONFIRMATION_REQUIRED_MESSAGE = "<red>Confirmation required. Confirm using /<command> confirm.";
    public static String NO_PENDING_COMMANDS_MESSAGE = "<red>You don't have any pending commands.";

    public static String HELP_COMMAND_DESCRIPTION = "Get help for Pl3xmap commands";
    public static String CONFIRM_COMMAND_DESCRIPTION = "Confirm a pending command";
    public static String RELOAD_COMMAND_DESCRIPTION = "Reloads the plugin";
    public static String FULLRENDER_COMMAND_DESCRIPTION = "Starts a full render for the specified world";
    public static String CANCEL_RENDER_COMMAND_DESCRIPTION = "Cancels a render for the specified world";
    public static String OPTIONAL_WORLD_ARGUMENT_DESCRIPTION = "Defaults to the players current world if not provided";
    public static String RADIUSRENDER_COMMAND_DESCRIPTION = "Starts a radius render";
    public static String HELP_QUERY_ARGUMENT_DESCRIPTION = "Help Query";

    public static String PLUGIN_RELOADED = "<green><name> v<version> reloaded";

    public static String PLUGIN_VERSION = "<green><name> v<version>";

    public static String LOG_STARTED_FULLRENDER = "<dark_aqua>Started full map render for <yellow><world>";
    public static String LOG_STARTED_RADIUSRENDER = "<dark_aqua>Started radius map render for <yellow><world>";
    public static String LOG_SCANNING_REGION_FILES = "<yellow>Scanning region files...";
    public static String LOG_FOUND_TOTAL_REGION_FILES = "<green>Found <gray><total> <green>region files";
    public static String LOG_FINISHED_RENDERING = "<dark_aqua>Finished rendering map for <yellow><world>";
    public static String LOG_RENDER_PROGRESS = "Render progress for world: <world>, <current_chunks>/<total_chunks> chunks (<percent>), Elapsed: <elapsed>, ETA: <eta>, Rate: <rate>cps";

    public static String LOG_INTERNAL_WEB_DISABLED = "<green>Internal webserver is disabled in config.yml";
    public static String LOG_INTERNAL_WEB_STARTED = "<green>Internal webserver running on <bind>:<port>";
    public static String LOG_INTERNAL_WEB_STOPPED = "<green>Internal webserver stopped";

    // Colorless console log messages
    public static String LOG_INTERNAL_WEB_START_ERROR = "Internal webserver could not start";
    public static String LOG_COULD_NOT_CREATE_DIR = "Could not create directory! {path}";
    public static String LOG_COULD_NOT_SAVE_REGION = "Could not save map for region {x},{z}";
    public static String LOG_UNABLE_TO_WRITE_TO_FILE = "Unable to write to {path}";
    public static String LOG_INTERNAL_WEB_STOP_ERROR = "An error occurred with the internal webserver";

    private static void init() {
        RENDER_IN_PROGRESS = getString("render-in-progress", RENDER_IN_PROGRESS);
        RENDER_NOT_IN_PROGRESS = getString("render-not-in-progress", RENDER_NOT_IN_PROGRESS);
        CANCELLED_RENDER = getString("cancelled-render", CANCELLED_RENDER);

        CONSOLE_MUST_SPECIFY_WORLD = getString("command.message.console-must-specify-world", CONSOLE_MUST_SPECIFY_WORLD);
        NO_SUCH_WORLD = getString("command.message.no-such-world", NO_SUCH_WORLD);
        MAP_NOT_ENABLED_FOR_WORLD = getString("command.message.map-not-enabled-for-world", MAP_NOT_ENABLED_FOR_WORLD);
        CONFIRMATION_REQUIRED_MESSAGE = getString("command.message.confirmation-required", CONFIRMATION_REQUIRED_MESSAGE);
        NO_PENDING_COMMANDS_MESSAGE = getString("command.message.no-pending-commands", NO_PENDING_COMMANDS_MESSAGE);

        HELP_COMMAND_DESCRIPTION = getString("command.description.help", HELP_COMMAND_DESCRIPTION);
        CONFIRM_COMMAND_DESCRIPTION = getString("command.description.confirm", CONFIRM_COMMAND_DESCRIPTION);
        RELOAD_COMMAND_DESCRIPTION = getString("command.description.reload", RELOAD_COMMAND_DESCRIPTION);
        FULLRENDER_COMMAND_DESCRIPTION = getString("command.description.full-render", FULLRENDER_COMMAND_DESCRIPTION);
        CANCEL_RENDER_COMMAND_DESCRIPTION = getString("command.description.cancel-render", CANCEL_RENDER_COMMAND_DESCRIPTION);
        RADIUSRENDER_COMMAND_DESCRIPTION = getString("command.description.radius-render", RADIUSRENDER_COMMAND_DESCRIPTION);
        OPTIONAL_WORLD_ARGUMENT_DESCRIPTION = getString("command.argument.optional-world", OPTIONAL_WORLD_ARGUMENT_DESCRIPTION);
        HELP_QUERY_ARGUMENT_DESCRIPTION = getString("command.argument.help-query", HELP_QUERY_ARGUMENT_DESCRIPTION);

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

        LOG_INTERNAL_WEB_DISABLED = getString("log.internal-web-disabled", LOG_INTERNAL_WEB_DISABLED);
        LOG_INTERNAL_WEB_STARTED = getString("log.internal-web-started", LOG_INTERNAL_WEB_STARTED);
        LOG_INTERNAL_WEB_START_ERROR = getString("log.internal-web-start-error", LOG_INTERNAL_WEB_START_ERROR);
        LOG_INTERNAL_WEB_STOPPED = getString("log.internal-web-stopped", LOG_INTERNAL_WEB_STOPPED);
        LOG_INTERNAL_WEB_STOP_ERROR = getString("log.internal-web-not-running", LOG_INTERNAL_WEB_STOP_ERROR);
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
        return config.getString(path, config.getString(path));
    }

    public static void send(final @NonNull CommandSender recipient, final @NonNull String miniMessage, final @NonNull Template @NonNull ... placeholders) {
        final Audience audience = Pl3xMapPlugin.getInstance().audiences().sender(recipient);
        audience.sendMessage(MiniMessage.get().parse(miniMessage, placeholders));
    }

    public static void send(final @NonNull CommandSender recipient, final @NonNull String miniMessage) {
        final Audience audience = Pl3xMapPlugin.getInstance().audiences().sender(recipient);
        audience.sendMessage(MiniMessage.get().parse(miniMessage));
    }

}
