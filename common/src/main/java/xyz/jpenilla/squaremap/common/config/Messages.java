package xyz.jpenilla.squaremap.common.config;

import cloud.commandframework.minecraft.extras.MinecraftHelp;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.StreamSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.Components;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;
import xyz.jpenilla.squaremap.common.util.SquaremapJarAccess;

@DefaultQualifier(NonNull.class)
@SuppressWarnings("unused") // Some messages are retrieved from the map instead of the field
public final class Messages {
    private static final Map<Class<?>, MessageFieldType<?>> MESSAGE_FIELD_TYPES = Map.of(
        ComponentMessage.class, ComponentMessage.FIELD_TYPE,
        StringMessage.class, StringMessage.FIELD_TYPE,
        String.class, StringMessage.STRING_FIELD_TYPE
    );

    private static final Map<String, Message> MESSAGES = new HashMap<>();

    // MiniMessage
    @MessageKey("render-in-progress")
    public static ComponentMessage RENDER_IN_PROGRESS = new ComponentMessage("<red>A render is already in progress on <world>");
    @MessageKey("render-not-in-progress")
    public static ComponentMessage RENDER_NOT_IN_PROGRESS = new ComponentMessage("<red>No renders running for <world>");
    @MessageKey("cancelled-render")
    public static ComponentMessage CANCELLED_RENDER = new ComponentMessage("<green>Render cancelled for <world>");
    @MessageKey("successfully-reset-map")
    public static ComponentMessage SUCCESSFULLY_RESET_MAP = new ComponentMessage("<green>Successfully reset map for <world>");
    @MessageKey("paused-renders")
    public static ComponentMessage PAUSED_RENDER = new ComponentMessage("<green>Paused renders for <world>");
    @MessageKey("unpaused-renders")
    public static ComponentMessage UNPAUSED_RENDER = new ComponentMessage("<green>Unpaused renders for <world>");

    @MessageKey("command.message.console-must-specify-player")
    public static ComponentMessage CONSOLE_MUST_SPECIFY_PLAYER = new ComponentMessage("<red>You must specify a target player when running this command from console");
    // todo
    @MessageKey("command.message.player-not-found-for-input")
    public static ComponentMessage PLAYER_NOT_FOUND_FOR_INPUT = new ComponentMessage("<red>No player found for input '<input>'");
    @MessageKey("command.message.console-must-specify-world")
    public static ComponentMessage CONSOLE_MUST_SPECIFY_WORLD = new ComponentMessage("<red>You must specify the world when running this command from console");
    @MessageKey("command.message.no-such-world")
    public static ComponentMessage NO_SUCH_WORLD = new ComponentMessage("No such world '<world>'");
    @MessageKey("command.message.map-not-enabled-for-world")
    public static ComponentMessage MAP_NOT_ENABLED_FOR_WORLD = new ComponentMessage("Map is not enabled for world '<world>'");
    @MessageKey("command.message.confirmation-required")
    public static ComponentMessage CONFIRMATION_REQUIRED_MESSAGE = new ComponentMessage("<red>Confirmation required. Confirm using /<command> confirm.");
    @MessageKey("command.message.no-pending-commands")
    public static ComponentMessage NO_PENDING_COMMANDS_MESSAGE = new ComponentMessage("<red>You don't have any pending commands.");
    @MessageKey("command.message.progresslogging.enabled")
    public static ComponentMessage PROGRESSLOGGING_ENABLED_MESSAGE = new ComponentMessage("<green>Render progress logging has been enabled.");
    @MessageKey("command.message.progresslogging.disabled")
    public static ComponentMessage PROGRESSLOGGING_DISABLED_MESSAGE = new ComponentMessage("<green>Render progress logging has been disabled.");
    @MessageKey("command.message.progresslogging.set-rate")
    public static ComponentMessage PROGRESSLOGGING_SET_RATE_MESSAGE = new ComponentMessage("<green>Render progress logging interval has been set to </green><seconds> seconds");
    @MessageKey("command.message.progresslogging.statusmessage")
    public static ComponentMessage PROGRESSLOGGING_STATUS_MESSAGE = new ComponentMessage("Render progress logging enabled: <enabled>, interval: <green><seconds></green> seconds");

    public static final String COMMAND_HELP_MESSAGE_PREFIX = "command.message.help.";

    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_HELP_TITLE)
    public static ComponentMessage COMMAND_HELP_MESSAGE_HELP_TITLE = new ComponentMessage("squaremap command help");
    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_COMMAND)
    public static ComponentMessage COMMAND_HELP_MESSAGE_COMMAND = new ComponentMessage("Command");
    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_DESCRIPTION)
    public static ComponentMessage COMMAND_HELP_MESSAGE_DESCRIPTION = new ComponentMessage("Description");
    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_NO_DESCRIPTION)
    public static ComponentMessage COMMAND_HELP_MESSAGE_NO_DESCRIPTION = new ComponentMessage("No description");
    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_ARGUMENTS)
    public static ComponentMessage COMMAND_HELP_MESSAGE_ARGUMENTS = new ComponentMessage("Arguments");
    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_OPTIONAL)
    public static ComponentMessage COMMAND_HELP_MESSAGE_OPTIONAL = new ComponentMessage("Optional");
    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_SHOWING_RESULTS_FOR_QUERY)
    public static ComponentMessage COMMAND_HELP_MESSAGE_SHOWING_RESULTS_FOR_QUERY = new ComponentMessage("Showing search results for query");
    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_NO_RESULTS_FOR_QUERY)
    public static ComponentMessage COMMAND_HELP_MESSAGE_NO_RESULTS_FOR_QUERY = new ComponentMessage("No results for query");
    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_AVAILABLE_COMMANDS)
    public static ComponentMessage COMMAND_HELP_MESSAGE_AVAILABLE_COMMANDS = new ComponentMessage("Available Commands");
    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_CLICK_TO_SHOW_HELP)
    public static ComponentMessage COMMAND_HELP_MESSAGE_CLICK_TO_SHOW_HELP = new ComponentMessage("Click to show help for this command");
    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_PAGE_OUT_OF_RANGE)
    public static ComponentMessage COMMAND_HELP_MESSAGE_PAGE_OUT_OF_RANGE = new ComponentMessage("Error: Page <page> is not in range. Must be in range [1, <max_pages>]");
    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_CLICK_FOR_NEXT_PAGE)
    public static ComponentMessage COMMAND_HELP_MESSAGE_CLICK_FOR_NEXT_PAGE = new ComponentMessage("Click for next page");
    @MessageKey(COMMAND_HELP_MESSAGE_PREFIX + MinecraftHelp.MESSAGE_CLICK_FOR_PREVIOUS_PAGE)
    public static ComponentMessage COMMAND_HELP_MESSAGE_CLICK_FOR_PREVIOUS_PAGE = new ComponentMessage("Click for previous page");

    private static final String COMMAND_EXCEPTION_MESSAGE_PREFIX = "command.message.exception.";

    @MessageKey(COMMAND_EXCEPTION_MESSAGE_PREFIX + "command-execution")
    public static ComponentMessage COMMAND_EXCEPTION_COMMAND_EXECUTION = new ComponentMessage("<red>An internal error occurred while attempting to perform this command.");
    @MessageKey(COMMAND_EXCEPTION_MESSAGE_PREFIX + "no-permission")
    public static ComponentMessage COMMAND_EXCEPTION_NO_PERMISSION = new ComponentMessage("<red>I'm sorry, but you do not have permission to perform this command.\n"
        + "Please contact the server administrators if you believe that this is in error.");
    @MessageKey(COMMAND_EXCEPTION_MESSAGE_PREFIX + "invalid-argument")
    public static ComponentMessage COMMAND_EXCEPTION_INVALID_ARGUMENT = new ComponentMessage("<red>Invalid command argument<white>:</white> <gray><message>");
    @MessageKey(COMMAND_EXCEPTION_MESSAGE_PREFIX + "invalid-sender-type")
    public static ComponentMessage COMMAND_EXCEPTION_INVALID_SENDER_TYPE = new ComponentMessage("<red>Invalid command sender type. You must be of type <gray><required_sender_type></gray>.");
    @MessageKey(COMMAND_EXCEPTION_MESSAGE_PREFIX + "invalid-syntax")
    public static ComponentMessage COMMAND_EXCEPTION_INVALID_SYNTAX = new ComponentMessage("<red>Invalid command syntax. Correct command syntax is<white>:</white> <gray><correct_syntax>");

    public static final String PARSER_EXCEPTION_MESSAGE_PREFIX = "command.message.parser-exception.";

    // We use _ in these keys for simplicity - it matches what cloud is using. We could normalize them, but seems like more effort than it's worth
    @MessageKey(PARSER_EXCEPTION_MESSAGE_PREFIX + "string")
    public static ComponentMessage STRING_PARSE_EXCEPTION = new ComponentMessage("'<input>' is not a valid string of type <string_mode>");
    @MessageKey(PARSER_EXCEPTION_MESSAGE_PREFIX + "number")
    public static ComponentMessage NUMBER_PARSE_EXCEPTION = new ComponentMessage("'<input>' is not a valid number in the range <min> to <max>");
    @MessageKey(PARSER_EXCEPTION_MESSAGE_PREFIX + "location.invalid_format")
    public static ComponentMessage LOCATION_PARSE_EXCEPTION_INVALID_FORMAT = new ComponentMessage("'<input>' is not a valid location. Required format is '<x> <z>'");
    @MessageKey(PARSER_EXCEPTION_MESSAGE_PREFIX + "location.mixed_local_absolute")
    public static ComponentMessage LOCATION_PARSE_EXCEPTION_MIXED_LOCAL_ABSOLUTE = new ComponentMessage("Cannot mix local and absolute coordinates. (either all coordinates use '^' or none do)");
    @MessageKey(PARSER_EXCEPTION_MESSAGE_PREFIX + "selector.malformed")
    public static ComponentMessage SELECTOR_PARSE_EXCEPTION_MALFORMED = new ComponentMessage("Selector '<input>' is malformed");
    @MessageKey(PARSER_EXCEPTION_MESSAGE_PREFIX + "selector.too_many_players")
    public static ComponentMessage SELECTOR_PARSE_EXCEPTION_TOO_MANY_PLAYERS = new ComponentMessage("More than 1 player selected in single player selector");
    @MessageKey(PARSER_EXCEPTION_MESSAGE_PREFIX + "selector.non_player_in_player_selector")
    public static ComponentMessage SELECTOR_PARSE_EXCEPTION_NON_PLAYER_IN_PLAYER_SELECTOR = new ComponentMessage("Non-player(s) selected in player selector");

    @MessageKey("click-to-copy")
    public static ComponentMessage CLICK_TO_COPY = new ComponentMessage("Click to copy to clipboard");
    @MessageKey("click-for-help")
    public static ComponentMessage CLICK_FOR_HELP = new ComponentMessage("Click for help");
    @MessageKey("click-to-confirm")
    public static ComponentMessage CLICK_TO_CONFIRM = new ComponentMessage("Click to confirm");
    @MessageKey("click-to-toggle")
    public static ComponentMessage CLICK_TO_TOGGLE = new ComponentMessage("Click to toggle");

    @MessageKey("command.prefix")
    public static ComponentMessage COMMAND_PREFIX = new ComponentMessage("<white>[<gradient:#C028FF:#5B00FF>squaremap</gradient>]</white> ");
    @MessageKey("command.description.help")
    public static ComponentMessage HELP_COMMAND_DESCRIPTION = new ComponentMessage("Get help for squaremap commands");
    @MessageKey("command.description.resetmap")
    public static ComponentMessage RESETMAP_COMMAND_DESCRIPTION = new ComponentMessage("Resets the map of a specified world");
    @MessageKey("command.description.confirm")
    public static ComponentMessage CONFIRM_COMMAND_DESCRIPTION = new ComponentMessage("Confirm a pending command");
    @MessageKey("command.description.reload")
    public static ComponentMessage RELOAD_COMMAND_DESCRIPTION = new ComponentMessage("Reloads the plugin");
    @MessageKey("command.description.full-render")
    public static ComponentMessage FULLRENDER_COMMAND_DESCRIPTION = new ComponentMessage("Starts a full render for the specified world");
    @MessageKey("command.description.cancel-render")
    public static ComponentMessage CANCEL_RENDER_COMMAND_DESCRIPTION = new ComponentMessage("Cancels a render for the specified world");
    @MessageKey("command.description.pause-render")
    public static ComponentMessage PAUSE_RENDER_COMMAND_DESCRIPTION = new ComponentMessage("Pauses all renders for the specified world");
    @MessageKey("command.argument.optional-world")
    public static ComponentMessage OPTIONAL_WORLD_ARGUMENT_DESCRIPTION = new ComponentMessage("Defaults to the players current world if not provided");
    @MessageKey("command.description.radius-render")
    public static ComponentMessage RADIUSRENDER_COMMAND_DESCRIPTION = new ComponentMessage("Starts a radius render");
    @MessageKey("command.description.progresslogging.status")
    public static ComponentMessage PROGRESSLOGGING_COMMAND_DESCRIPTION = new ComponentMessage("Prints the current settings for render progress logging");
    @MessageKey("command.description.progresslogging.toggle")
    public static ComponentMessage PROGRESSLOGGING_TOGGLE_COMMAND_DESCRIPTION = new ComponentMessage("Toggles render progress logging on or off");
    @MessageKey("command.description.progresslogging.rate")
    public static ComponentMessage PROGRESSLOGGING_RATE_COMMAND_DESCRIPTION = new ComponentMessage("Sets the interval in seconds for logging render progress");
    @MessageKey("command.argument.optional-center")
    public static ComponentMessage OPTIONAL_CENTER_ARGUMENT_DESCRIPTION = new ComponentMessage("Defaults to (<white>0<gray>,</gray> 0</white>) if unspecified");
    @MessageKey("command.argument.optional-player")
    public static ComponentMessage OPTIONAL_PLAYER_ARGUMENT_DESCRIPTION = new ComponentMessage("Defaults to the executing player if unspecified (console must specify a player)");
    @MessageKey("command.argument.help-query")
    public static ComponentMessage HELP_QUERY_ARGUMENT_DESCRIPTION = new ComponentMessage("Help Query");

    @MessageKey("command.description.hide")
    public static ComponentMessage HIDE_COMMAND_DESCRIPTION = new ComponentMessage("Hide a player from the map view");
    @MessageKey("command.hide.already-hidden")
    public static ComponentMessage PLAYER_ALREADY_HIDDEN = new ComponentMessage("<red><player> is already hidden from map");
    @MessageKey("command.hide.hidden")
    public static ComponentMessage PLAYER_HIDDEN = new ComponentMessage("<green><player> is now hidden from map");

    @MessageKey("command.description.show")
    public static ComponentMessage SHOW_COMMAND_DESCRIPTION = new ComponentMessage("Show a player on the map view");
    @MessageKey("command.show.not-hidden")
    public static ComponentMessage PLAYER_NOT_HIDDEN = new ComponentMessage("<red><player> is not hidden from map");
    @MessageKey("command.show.shown")
    public static ComponentMessage PLAYER_SHOWN = new ComponentMessage("<green><player> is no longer hidden from map");

    @MessageKey("plugin-reloaded")
    public static ComponentMessage PLUGIN_RELOADED = new ComponentMessage("<green><name> v<version> reloaded");

    // Web UI messages
    @MessageKey("ui.page-title")
    public static String UI_TITLE = "squaremap - {world}";
    @MessageKey("ui.coordinates")
    public static String UI_COORDINATES_HTML = "Coordinates<br/>{x}, {z}";
    @MessageKey("ui.sidebar.player-list")
    public static String UI_SIDEBAR_PLAYER_LIST_LABEL = "Players ({cur}/{max})";
    @MessageKey("ui.sidebar.world-list")
    public static String UI_SIDEBAR_WORLD_LIST_LABEL = "Worlds";
    @MessageKey("ui.layers.player-tracker")
    public static String UI_PLAYER_TRACKER_LABEL = "Players";
    @MessageKey("ui.layers.world-spawn")
    public static String UI_SPAWN_MARKER_ICON_LABEL = "Spawn";
    @MessageKey("ui.layers.world-border")
    public static String UI_WORLDBORDER_MARKER_LABEL = "World Border";

    // Console log messages
    @MessageKey("log.started-full-render")
    public static String LOG_STARTED_FULLRENDER = "Started full map render for <world>";
    @MessageKey("log.started-radius-render")
    public static String LOG_STARTED_RADIUSRENDER = "Started radius map render for <world>";
    @MessageKey("log.scanning-region-files")
    public static String LOG_SCANNING_REGION_FILES = "Scanning region files... (this may take a moment)";
    @MessageKey("log.found-total-region-files")
    public static String LOG_FOUND_TOTAL_REGION_FILES = "Found <total> region files";
    @MessageKey("log.finished-rendering")
    public static String LOG_FINISHED_RENDERING = "Finished rendering map for <world>";
    @MessageKey("log.cancelled-rendering")
    public static String LOG_CANCELLED_RENDERING = "Rendering map for <world> has been interrupted";
    @MessageKey("log.resumed-rendering")
    public static String LOG_RESUMED_RENDERING = "Rendering map for <world> has been resumed";
    @MessageKey("log.scanning-region-progress")
    public static String LOG_RENDER_PROGRESS = "(<percent>) World: <world> Chunks: <current_chunks>/<total_chunks> Elapsed: <elapsed> ETA: <eta> Rate: <rate> cps";
    @MessageKey("log.scanning-region-progress-with-regions")
    public static String LOG_RENDER_PROGRESS_WITH_REGIONS = "(<percent>) World: <world> Regions: <current_regions>/<total_regions> Chunks: <current_chunks>/<total_chunks> Elapsed: <elapsed> ETA: <eta> Rate: <rate> cps";

    @MessageKey("log.internal-web-disabled")
    public static String LOG_INTERNAL_WEB_DISABLED = "Internal webserver is disabled in config.yml";
    @MessageKey("log.internal-web-started")
    public static String LOG_INTERNAL_WEB_STARTED = "Internal webserver running on <bind>:<port>";
    @MessageKey("log.internal-web-stopped")
    public static String LOG_INTERNAL_WEB_STOPPED = "Internal webserver stopped";

    @MessageKey("log.internal-web-start-error")
    public static String LOG_INTERNAL_WEB_START_ERROR = "Internal webserver could not start";
    @MessageKey("log.could-not-create-directory")
    public static String LOG_COULD_NOT_CREATE_DIR = "Could not create directory! <path>";
    @MessageKey("log.could-not-read-region")
    public static String LOG_COULD_NOT_READ_REGION = "Could not read map image for region <x>,<z> (image corrupted?). It will be overwritten.";
    @MessageKey("log.could-not-save-region")
    public static String LOG_COULD_NOT_SAVE_REGION = "Could not save map for region <x>,<z>";
    @MessageKey("log.internal-web-not-running")
    public static String LOG_INTERNAL_WEB_STOP_ERROR = "An error occurred with the internal webserver";

    @MessageKey("log.update-checker.fetching-version-information")
    public static String UPDATE_CHECKER_FETCHING_VERSION_INFORMATION = "Fetching version information...";
    @MessageKey("log.update-checker.behind-branch")
    public static String UPDATE_CHECKER_BEHIND_BRANCH = "squaremap is <behind> version(s) behind branch '<branch>'!";
    @MessageKey("log.update-checker.download-dev-builds")
    public static String UPDATE_CHECKER_DOWNLOAD_DEV_BUILDS = "Download dev builds from <link>";
    @MessageKey("log.update-checker.unknown-commit")
    public static String UPDATE_CHECKER_UNKNOWN_COMMIT = "Unknown commit '<commit>', cannot check for updates.";
    @MessageKey("log.update-checker.behind-releases")
    public static String UPDATE_CHECKER_BEHIND_RELEASES = "squaremap is <behind> version(s) out of date.";
    @MessageKey("log.update-checker.download-release")
    public static String UPDATE_CHECKER_DOWNLOAD_RELEASE = "Download the latest release (<latest>) from <link>";

    private Messages() {
    }

    public static void reload(
        final SquaremapJarAccess squaremapJar,
        final DirectoryProvider directoryProvider
    ) {
        squaremapJar.extract("locale", directoryProvider.localeDirectory(), false);

        final Path configFile = directoryProvider.localeDirectory().resolve(Config.LANGUAGE_FILE);
        final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .path(configFile)
            .nodeStyle(NodeStyle.BLOCK)
            .build();
        final CommentedConfigurationNode config;
        try {
            config = loader.load();
        } catch (final ConfigurateException ex) {
            throw new RuntimeException("Could not load " + Config.LANGUAGE_FILE + ", please correct your syntax errors", ex);
        }

        MESSAGES.clear();
        loadValues(Messages.class, config);

        try {
            loader.save(config);
        } catch (final ConfigurateException ex) {
            Logging.logger().error("Could not save " + configFile, ex);
        }
    }

    private static void loadValues(final Class<?> clazz, final CommentedConfigurationNode config) {
        Arrays.stream(clazz.getDeclaredFields())
            .filter(ReflectionUtil::isStatic)
            .filter(Messages::checkTypeAndAnnotation)
            .forEach(field -> loadValue(config, field));
    }

    private static boolean checkTypeAndAnnotation(final Field field) {
        if (!field.isAnnotationPresent(MessageKey.class)) {
            return false;
        }
        if (MESSAGE_FIELD_TYPES.containsKey(field.getType())) {
            return true;
        } else {
            Logging.logger().warn("Field '{}.{}' of type '{}' is annotated with @MessageKey, but is not a supported MessageFieldType.", field.getDeclaringClass().getName(), field.getName(), field.getType().getName());
            return false;
        }
    }

    private static void loadValue(final CommentedConfigurationNode config, final Field field) {
        final MessageKey messageKey = field.getAnnotation(MessageKey.class);
        try {
            loadValue(config, field, messageKey);
        } catch (final Exception ex) {
            Logging.logger().warn("Failed to load message with key '{}' from '{}'", messageKey.value(), Config.LANGUAGE_FILE, ex);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void loadValue(
        final CommentedConfigurationNode config,
        final Field field,
        final MessageKey messageKey
    ) throws ReflectiveOperationException {
        final @Nullable MessageFieldType<?> messageFieldType = MESSAGE_FIELD_TYPES.get(field.getType());
        if (messageFieldType == null) {
            throw new IllegalStateException("Could not find MessageFieldType for field with type " + field.getType().getName());
        }
        final String defaultValue = !messageKey.defaultValue().equals("")
            ? messageKey.defaultValue()
            : (String) ((Function) messageFieldType.defaultGetter()).apply(field.get(null));
        final MessageFieldType.ReadResult<?> readResult = messageFieldType.messageFactory().apply(getString(config, messageKey.value(), defaultValue));
        field.set(null, readResult.fieldValue());
        MESSAGES.put(messageKey.value(), readResult.message());
    }

    private static String getString(final ConfigurationNode node, final String path, final String def) {
        return node.node(AbstractConfig.splitPath(path)).getString(def);
    }

    public static ComponentMessage componentMessage(final String key) {
        final @Nullable Message message = MESSAGES.get(key);
        if (message == null) {
            throw new IllegalArgumentException("Message with key '" + key + "' does not exist!");
        }
        if (!(message instanceof ComponentMessage componentMessage)) {
            throw new IllegalArgumentException("Message with key '" + key + "' is not a ComponentMessage!");
        }
        return componentMessage;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface MessageKey {
        String value();

        String defaultValue() default "";
    }

    public sealed interface Message permits StringMessage, ComponentMessage {
    }

    public record StringMessage(String message) implements Message {
        private static final MessageFieldType<StringMessage> FIELD_TYPE = MessageFieldType.create(StringMessage::new, StringMessage::message);
        private static final MessageFieldType<String> STRING_FIELD_TYPE = new MessageFieldType<>(
            string -> new MessageFieldType.ReadResult<>(new StringMessage(string), string),
            UnaryOperator.identity()
        );

        public String withPlaceholders(final Object... keyValuePairs) {
            return Logging.replace(this.message, keyValuePairs);
        }
    }

    public static final class ComponentMessage implements Message, ComponentLike {
        private static final MessageFieldType<ComponentMessage> FIELD_TYPE = MessageFieldType.create(ComponentMessage::new, ComponentMessage::miniMessage);

        private final String miniMessage;
        private volatile @MonotonicNonNull Component noPlaceholders;

        private ComponentMessage(final String miniMessage) {
            this.miniMessage = miniMessage;
        }

        public Component withPlaceholders(final TagResolver... placeholders) {
            if (placeholders.length == 0) {
                return this.asComponent();
            }
            return Components.miniMessage(this.miniMessage, placeholders);
        }

        public Component withPlaceholders(final Iterable<TagResolver> placeholders) {
            return Components.miniMessage(this.miniMessage, StreamSupport.stream(placeholders.spliterator(), false).toArray(TagResolver[]::new));
        }

        @Override
        public Component asComponent() {
            if (this.noPlaceholders == null) {
                synchronized (this) {
                    if (this.noPlaceholders == null) {
                        this.noPlaceholders = Components.miniMessage(this.miniMessage);
                    }
                }
            }
            return this.noPlaceholders;
        }

        public String miniMessage() {
            return this.miniMessage;
        }
    }

    private record MessageFieldType<T>(
        Function<String, ReadResult<T>> messageFactory,
        Function<T, String> defaultGetter
    ) {
        static <T extends Message> MessageFieldType<T> create(
            Function<String, T> messageFactory,
            Function<T, String> defaultGetter
        ) {
            return new MessageFieldType<>(
                string -> {
                    final T message = messageFactory.apply(string);
                    return new ReadResult<>(message, message);
                },
                defaultGetter
            );
        }

        record ReadResult<T>(Message message, T fieldValue) {
        }
    }
}
