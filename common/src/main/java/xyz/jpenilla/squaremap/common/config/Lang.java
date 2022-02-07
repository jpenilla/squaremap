package xyz.jpenilla.squaremap.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.Template;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.util.Components;
import xyz.jpenilla.squaremap.common.util.FileUtil;

@DefaultQualifier(NonNull.class)
public final class Lang {

    // MiniMessage formatted strings, to be sent using Logger.info(String, Template...) or Lang.send(CommandSender, String, Template...)
    @LangKey("render-in-progress")
    public static String RENDER_IN_PROGRESS = "<red>A render is already in progress on <world>";
    @LangKey("render-not-in-progress")
    public static String RENDER_NOT_IN_PROGRESS = "<red>No renders running for <world>";
    @LangKey("cancelled-render")
    public static String CANCELLED_RENDER = "<green>Render cancelled for <world>";
    @LangKey("successfully-reset-map")
    public static String SUCCESSFULLY_RESET_MAP = "<green>Successfully reset map for <world>";
    @LangKey("paused-renders")
    public static String PAUSED_RENDER = "<green>Paused renders for <world>";
    @LangKey("unpaused-renders")
    public static String UNPAUSED_RENDER = "<green>Unpaused renders for <world>";

    @LangKey("command.message.console-must-specify-player")
    public static String CONSOLE_MUST_SPECIFY_PLAYER = "<red>You must specify a target player when running this command from console";
    @LangKey("command.message.player-not-found-for-input")
    public static String PLAYER_NOT_FOUND_FOR_INPUT = "<red>No player found for input '<input>'";
    @LangKey("command.message.console-must-specify-world")
    public static String CONSOLE_MUST_SPECIFY_WORLD = "<red>You must specify the world when running this command from console";
    @LangKey("command.message.no-such-world")
    public static String NO_SUCH_WORLD = "<red>No such world '<world>'";
    @LangKey("command.message.map-not-enabled-for-world")
    public static String MAP_NOT_ENABLED_FOR_WORLD = "<red>Map is not enabled for world '<world>'";
    @LangKey("command.message.confirmation-required")
    public static String CONFIRMATION_REQUIRED_MESSAGE = "<red>Confirmation required. Confirm using /<command> confirm.";
    @LangKey("command.message.no-pending-commands")
    public static String NO_PENDING_COMMANDS_MESSAGE = "<red>You don't have any pending commands.";
    @LangKey("command.message.progresslogging.enabled")
    public static String PROGRESSLOGGING_ENABLED_MESSAGE = "<green>Render progress logging has been enabled.";
    @LangKey("command.message.progresslogging.disabled")
    public static String PROGRESSLOGGING_DISABLED_MESSAGE = "<green>Render progress logging has been disabled.";
    @LangKey("command.message.progresslogging.set-rate")
    public static String PROGRESSLOGGING_SET_RATE_MESSAGE = "<green>Render progress logging interval has been set to </green><seconds> seconds";
    @LangKey("command.message.progresslogging.statusmessage")
    public static String PROGRESSLOGGING_STATUS_MESSAGE = "Render progress logging enabled: <enabled>, interval: <green><seconds></green> seconds";

    @LangKey("click-for-help")
    public static String CLICK_FOR_HELP = "Click for help";
    @LangKey("click-to-confirm")
    public static String CLICK_TO_CONFIRM = "Click to confirm";
    @LangKey("click-to-toggle")
    public static String CLICK_TO_TOGGLE = "Click to toggle";

    @LangKey("command.prefix")
    public static String COMMAND_PREFIX = "<white>[<gradient:#C028FF:#5B00FF>squaremap</gradient>]</white> ";
    @LangKey("command.description.help")
    public static String HELP_COMMAND_DESCRIPTION = "Get help for squaremap commands";
    @LangKey("command.description.resetmap")
    public static String RESETMAP_COMMAND_DESCRIPTION = "Resets the map of a specified world";
    @LangKey("command.description.confirm")
    public static String CONFIRM_COMMAND_DESCRIPTION = "Confirm a pending command";
    @LangKey("command.description.reload")
    public static String RELOAD_COMMAND_DESCRIPTION = "Reloads the plugin";
    @LangKey("command.description.full-render")
    public static String FULLRENDER_COMMAND_DESCRIPTION = "Starts a full render for the specified world";
    @LangKey("command.description.cancel-render")
    public static String CANCEL_RENDER_COMMAND_DESCRIPTION = "Cancels a render for the specified world";
    @LangKey("command.description.pause-render")
    public static String PAUSE_RENDER_COMMAND_DESCRIPTION = "Pauses all renders for the specified world";
    @LangKey("command.argument.optional-world")
    public static String OPTIONAL_WORLD_ARGUMENT_DESCRIPTION = "Defaults to the players current world if not provided";
    @LangKey("command.description.radius-render")
    public static String RADIUSRENDER_COMMAND_DESCRIPTION = "Starts a radius render";
    @LangKey("command.description.progresslogging.status")
    public static String PROGRESSLOGGING_COMMAND_DESCRIPTION = "Prints the current settings for render progress logging";
    @LangKey("command.description.progresslogging.toggle")
    public static String PROGRESSLOGGING_TOGGLE_COMMAND_DESCRIPTION = "Toggles render progress logging on or off";
    @LangKey("command.description.progresslogging.rate")
    public static String PROGRESSLOGGING_RATE_COMMAND_DESCRIPTION = "Sets the interval in seconds for logging render progress";
    @LangKey("command.argument.optional-center")
    public static String OPTIONAL_CENTER_ARGUMENT_DESCRIPTION = "Defaults to (<white>0<gray>,</gray> 0</white>) if unspecified";
    @LangKey("command.argument.optional-player")
    public static String OPTIONAL_PLAYER_ARGUMENT_DESCRIPTION = "Defaults to the executing player if unspecified (console must specify a player)";
    @LangKey("command.argument.help-query")
    public static String HELP_QUERY_ARGUMENT_DESCRIPTION = "Help Query";

    @LangKey("command.description.hide")
    public static String HIDE_COMMAND_DESCRIPTION = "Hide a player from the map view";
    @LangKey("command.hide.already-hidden")
    public static String PLAYER_ALREADY_HIDDEN = "<red><player> is already hidden from map";
    @LangKey("command.hide.hidden")
    public static String PLAYER_HIDDEN = "<green><player> is now hidden from map";

    @LangKey("command.description.show")
    public static String SHOW_COMMAND_DESCRIPTION = "Show a player on the map view";
    @LangKey("command.show.not-hidden")
    public static String PLAYER_NOT_HIDDEN = "<red><player> is not hidden from map";
    @LangKey("command.show.shown")
    public static String PLAYER_SHOWN = "<green><player> is no longer hidden from map";

    @LangKey("ui.page-title")
    public static String UI_TITLE = "squaremap - {world}";
    @LangKey("ui.coordinates")
    public static String UI_COORDINATES_HTML = "Coordinates<br/>{x}, {z}";
    @LangKey("ui.sidebar.player-list")
    public static String UI_SIDEBAR_PLAYER_LIST_LABEL = "Players ({cur}/{max})";
    @LangKey("ui.sidebar.world-list")
    public static String UI_SIDEBAR_WORLD_LIST_LABEL = "Worlds";
    @LangKey("ui.layers.player-tracker")
    public static String UI_PLAYER_TRACKER_LABEL = "Players";
    @LangKey("ui.layers.world-spawn")
    public static String UI_SPAWN_MARKER_ICON_LABEL = "Spawn";
    @LangKey("ui.layers.world-border")
    public static String UI_WORLDBORDER_MARKER_LABEL = "World Border";

    @LangKey("plugin-reloaded")
    public static String PLUGIN_RELOADED = "<green><name> v<version> reloaded";

    // Colorless console log messages
    @LangKey("log.started-full-render")
    public static String LOG_STARTED_FULLRENDER = "Started full map render for <world>";
    @LangKey("log.started-radius-render")
    public static String LOG_STARTED_RADIUSRENDER = "Started radius map render for <world>";
    @LangKey("log.scanning-region-files")
    public static String LOG_SCANNING_REGION_FILES = "Scanning region files... (this may take a moment)";
    @LangKey("log.found-total-region-files")
    public static String LOG_FOUND_TOTAL_REGION_FILES = "Found <total> region files";
    @LangKey("log.finished-rendering")
    public static String LOG_FINISHED_RENDERING = "Finished rendering map for <world>";
    @LangKey("log.cancelled-rendering")
    public static String LOG_CANCELLED_RENDERING = "Rendering map for <world> has been interrupted";
    @LangKey("log.resumed-rendering")
    public static String LOG_RESUMED_RENDERING = "Rendering map for <world> has been resumed";
    @LangKey("log.scanning-region-progress")
    public static String LOG_RENDER_PROGRESS = "(<percent>) World: <world> Chunks: <current_chunks>/<total_chunks> Elapsed: <elapsed> ETA: <eta> Rate: <rate> cps";
    @LangKey("log.scanning-region-progress-with-regions")
    public static String LOG_RENDER_PROGRESS_WITH_REGIONS = "(<percent>) World: <world> Regions: <current_regions>/<total_regions> Chunks: <current_chunks>/<total_chunks> Elapsed: <elapsed> ETA: <eta> Rate: <rate> cps";

    @LangKey("log.internal-web-disabled")
    public static String LOG_INTERNAL_WEB_DISABLED = "Internal webserver is disabled in config.yml";
    @LangKey("log.internal-web-started")
    public static String LOG_INTERNAL_WEB_STARTED = "Internal webserver running on <bind>:<port>";
    @LangKey("log.internal-web-stopped")
    public static String LOG_INTERNAL_WEB_STOPPED = "Internal webserver stopped";

    @LangKey("log.internal-web-start-error")
    public static String LOG_INTERNAL_WEB_START_ERROR = "Internal webserver could not start";
    @LangKey("log.could-not-create-directory")
    public static String LOG_COULD_NOT_CREATE_DIR = "Could not create directory! <path>";
    @LangKey("log.could-not-read-region")
    public static String LOG_COULD_NOT_READ_REGION = "Could not read map image for region <x>,<z> (image corrupted?). It will be overwritten.";
    @LangKey("log.could-not-save-region")
    public static String LOG_COULD_NOT_SAVE_REGION = "Could not save map for region <x>,<z>";
    @LangKey("log.internal-web-not-running")
    public static String LOG_INTERNAL_WEB_STOP_ERROR = "An error occurred with the internal webserver";

    @LangKey("log.update-checker.fetching-version-information")
    public static String UPDATE_CHECKER_FETCHING_VERSION_INFORMATION = "Fetching version information...";
    @LangKey("log.update-checker.behind-branch")
    public static String UPDATE_CHECKER_BEHIND_BRANCH = "squaremap is <behind> version(s) behind branch '<branch>'!";
    @LangKey("log.update-checker.download-dev-builds")
    public static String UPDATE_CHECKER_DOWNLOAD_DEV_BUILDS = "Download dev builds from <link>";
    @LangKey("log.update-checker.unknown-commit")
    public static String UPDATE_CHECKER_UNKNOWN_COMMIT = "Unknown commit '<commit>', cannot check for updates.";
    @LangKey("log.update-checker.behind-releases")
    public static String UPDATE_CHECKER_BEHIND_RELEASES = "squaremap is <behind> version(s) out of date.";
    @LangKey("log.update-checker.download-release")
    public static String UPDATE_CHECKER_DOWNLOAD_RELEASE = "Download the latest release (<latest>) from <link>";

    private static void init() {
        Arrays.stream(Lang.class.getDeclaredFields())
            .filter(Lang::annotatedString)
            .forEach(Lang::loadValue);
    }

    private static boolean annotatedString(final Field field) {
        return field.getType().equals(String.class)
            && field.getDeclaredAnnotation(LangKey.class) != null;
    }

    private static void loadValue(final Field field) {
        final LangKey langKey = field.getDeclaredAnnotation(LangKey.class);
        try {
            field.set(null, getString(langKey.value(), (String) field.get(null)));
        } catch (IllegalAccessException e) {
            Logging.logger().warn("Failed to load {}", Config.LANGUAGE_FILE, e);
        }
    }

    public static void reload() {
        final Path configFile = FileUtil.LOCALE_DIR.resolve(Config.LANGUAGE_FILE);
        final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .path(configFile)
            .nodeStyle(NodeStyle.BLOCK)
            .build();
        try {
            config = loader.load();
        } catch (ConfigurateException ex) {
            throw new RuntimeException("Could not load " + Config.LANGUAGE_FILE + ", please correct your syntax errors", ex);
        }

        init();

        try {
            loader.save(config);
        } catch (ConfigurateException ex) {
            Logging.logger().error("Could not save " + configFile, ex);
        }
    }

    private static @MonotonicNonNull ConfigurationNode config;

    private static String getString(String path, String def) {
        return config.node(AbstractConfig.splitPath(path)).getString(def);
    }

    public static void send(final Audience recipient, final String miniMessage, final Template... placeholders) {
        recipient.sendMessage(Components.miniMessage(miniMessage, placeholders));
    }

    public static void send(final Audience recipient, final String miniMessage) {
        recipient.sendMessage(Components.miniMessage(miniMessage));
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface LangKey {
        String value();
    }

}
