package xyz.jpenilla.squaremap.common.config;

import java.util.ArrayList;
import java.util.List;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.data.image.BufferedImageMapImageIO;
import xyz.jpenilla.squaremap.common.data.image.MapImageIO;
import xyz.jpenilla.squaremap.common.data.image.PNJMapImageIO;

@SuppressWarnings("unused")
public final class Config extends AbstractConfig {
    private static final int LATEST_VERSION = 3;

    Config(final DirectoryProvider directoryProvider) {
        super(directoryProvider.dataDirectory(), Config.class, "config.yml", LATEST_VERSION);
    }

    @Override
    protected void addVersions(final ConfigurationTransformation.VersionedBuilder versionedBuilder) {
        final ConfigurationTransformation oneToTwo = ConfigurationTransformation.builder()
            .addAction(NodePath.path("settings", "image-quality", "compress-images"), (path, node) -> {
                final double d = node.getDouble();
                node.raw(null);
                node.node("enabled").set(d != 0.0D);
                node.node("value").set(d == 0.0D ? d : 1.0D - d);
                return null;
            })
            .build();

        final ConfigurationTransformation twoToThree = ConfigurationTransformation.chain(
            ConfigurationTransformation.builder()
                .addAction(NodePath.path("settings", "image-quality"), (path, node) -> new Object[]{"settings", "image-io"})
                .build(),
            ConfigurationTransformation.builder()
                .addAction(NodePath.path("settings", "image-io", "compress-images"), (path, node) -> new Object[]{"settings", "image-io", "bufferedimage", "compress-images"})
                .build(),
            ConfigurationTransformation.builder()
                .addAction(NodePath.path("settings", "image-io"), (path, node) -> {
                    final boolean compress = node.node("bufferedimage", "compress-images", "enabled").getBoolean();
                    if (compress) {
                        node.node("backend").set("bufferedimage");
                    }
                    return null;
                })
                .build()
        );

        versionedBuilder.addVersion(2, oneToTwo);
        versionedBuilder.addVersion(LATEST_VERSION, twoToThree);
    }

    static Config config;

    public static void reload(final DirectoryProvider directoryProvider) {
        config = new Config(directoryProvider);
        config.readConfig(Config.class, null);
    }

    public static String LANGUAGE_FILE = "lang-en.yml";
    public static boolean DEBUG_MODE = false;
    public static boolean UPDATE_CHECKER = true;
    public static String WEB_ADDRESS = "http://localhost:8080";

    private static void baseSettings() {
        LANGUAGE_FILE = config.getString("settings.language-file", LANGUAGE_FILE);
        DEBUG_MODE = config.getBoolean("settings.debug-mode", DEBUG_MODE);
        UPDATE_CHECKER = config.getBoolean("settings.update-checker", UPDATE_CHECKER);
        WEB_ADDRESS = config.getString("settings.web-address", WEB_ADDRESS);
    }

    public static String WEB_DIR = "web";
    public static boolean UPDATE_WEB_DIR = true;

    private static void webDirSettings() {
        WEB_DIR = config.getString("settings.web-directory.path", WEB_DIR);
        UPDATE_WEB_DIR = config.getBoolean("settings.web-directory.auto-update", UPDATE_WEB_DIR);
    }

    public static boolean COMPRESS_IMAGES = false;
    private static double COMPRESSION_RATIO_CONFIG = 0.0F;
    public static float COMPRESSION_RATIO;
    public static MapImageIO<?> MAP_IMAGE_IO;

    private static void imageIOSettings() {
        COMPRESS_IMAGES = config.getBoolean("settings.image-io.bufferedimage.compress-images.enabled", COMPRESS_IMAGES);
        COMPRESSION_RATIO_CONFIG = config.getDouble("settings.image-io.bufferedimage.compress-images.value", COMPRESSION_RATIO_CONFIG);
        COMPRESSION_RATIO = (float) (1.0D - COMPRESSION_RATIO_CONFIG);
        final String imageIoBackend = config.getString("settings.image-io.backend", "pnj");
        MAP_IMAGE_IO = switch (imageIoBackend) {
            case "bufferedimage" -> new BufferedImageMapImageIO();
            case "pnj" -> new PNJMapImageIO();
            default -> throw new IllegalArgumentException("Invaild image io backend: " + imageIoBackend + "; Supported options: [pnj, bufferedimage]");
        };
    }

    public static boolean HTTPD_ENABLED = true;
    public static String HTTPD_BIND = "0.0.0.0";
    public static int HTTPD_PORT = 8080;
    public static boolean FLUSH_JSON_IMMEDIATELY = false;

    private static void internalWebServerSettings() {
        HTTPD_ENABLED = config.getBoolean("settings.internal-webserver.enabled", HTTPD_ENABLED);
        HTTPD_BIND = config.getString("settings.internal-webserver.bind", HTTPD_BIND);
        HTTPD_PORT = config.getInt("settings.internal-webserver.port", HTTPD_PORT);
        FLUSH_JSON_IMMEDIATELY = config.getBoolean("settings.internal-webserver.flush-json-immediately", FLUSH_JSON_IMMEDIATELY);
    }

    public static boolean UI_COORDINATES_ENABLED = true;
    public static boolean UI_LINK_ENABLED = true;
    public static String UI_SIDEBAR_PINNED = "unpinned";

    private static void uiSettings() {
        UI_COORDINATES_ENABLED = config.getBoolean("settings.ui.coordinates.enabled", UI_COORDINATES_ENABLED);
        UI_LINK_ENABLED = config.getBoolean("settings.ui.link.enabled", UI_LINK_ENABLED);
        UI_SIDEBAR_PINNED = config.getString("settings.ui.sidebar.pinned", UI_SIDEBAR_PINNED);
    }

    public static String MAIN_COMMAND_LABEL = "squaremap";
    public static final List<String> MAIN_COMMAND_ALIASES = new ArrayList<>();

    private static void commandSettings() {
        MAIN_COMMAND_LABEL = config.getString("settings.commands.main-command-label", MAIN_COMMAND_LABEL);
        MAIN_COMMAND_ALIASES.clear();
        MAIN_COMMAND_ALIASES.addAll(config.getList(String.class, "settings.commands.main-command-aliases", List.of("map")));
    }

    public static void toggleProgressLogging() {
        PROGRESS_LOGGING = !PROGRESS_LOGGING;
        config.set("settings.render-progress-logging.enabled", PROGRESS_LOGGING);
        config.save();
    }

    public static void setLoggingInterval(final int rate) {
        PROGRESS_LOGGING_INTERVAL = rate;
        config.set("settings.render-progress-logging.interval-seconds", rate);
        config.save();
    }

    public static volatile boolean PROGRESS_LOGGING;
    public static volatile int PROGRESS_LOGGING_INTERVAL;

    private static void progressLogging() {
        PROGRESS_LOGGING = config.getBoolean("settings.render-progress-logging.enabled", true);
        PROGRESS_LOGGING_INTERVAL = config.getInt("settings.render-progress-logging.interval-seconds", 1);
    }

    public static Config config() {
        return config;
    }

}
