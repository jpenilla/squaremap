package xyz.jpenilla.squaremap.common;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import javax.imageio.ImageIO;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.player.AbstractPlayerManager;
import xyz.jpenilla.squaremap.common.render.scanning.BiomeColorTables;
import xyz.jpenilla.squaremap.common.updatechecker.UpdateChecker;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;
import xyz.jpenilla.squaremap.common.util.SquaremapJarAccess;
import xyz.jpenilla.squaremap.common.util.text.Components;
import xyz.jpenilla.squaremap.common.web.WebJsonStore;
import xyz.jpenilla.squaremap.common.web.httpd.EmbeddedWebServer;
import xyz.jpenilla.squaremap.common.world.WorldManagerImpl;
import xyz.jpenilla.squaremap.common.world.layer.SpawnIconLayer;

@DefaultQualifier(NonNull.class)
@Singleton
public final class SquaremapCommon {
    private final Injector injector;
    private final SquaremapPlatform platform;
    private final SquaremapDirectories directories;
    private final ConfigManager configManager;
    private final AbstractPlayerManager playerManager;
    private final WorldManagerImpl worldManager;
    private final Commands commands;
    private final SquaremapJarAccess squaremapJar;
    private final WebJsonStore jsonStore;

    @Inject
    private SquaremapCommon(
        final Injector injector,
        final SquaremapPlatform platform,
        final SquaremapDirectories directories,
        final ConfigManager configManager,
        final AbstractPlayerManager playerManager,
        final WorldManagerImpl worldManager,
        final Commands commands,
        final SquaremapJarAccess squaremapJar,
        final WebJsonStore jsonStore
    ) {
        this.injector = injector;
        this.platform = platform;
        this.directories = directories;
        this.configManager = configManager;
        this.playerManager = playerManager;
        this.worldManager = worldManager;
        this.commands = commands;
        this.squaremapJar = squaremapJar;
        this.jsonStore = jsonStore;
    }

    public void init() {
        this.configManager.init();
        this.directories.init();
        this.start();
        this.setupApi();
        this.commands.registerCommands();
    }

    private void start() {
        this.squaremapJar.extract("web", this.directories.webDirectory(), Config.UPDATE_WEB_DIR);
        BiomeColorTables.loadImages(this.directories);
        this.worldManager.start();
        this.platform.startCallback();
        if (Config.HTTPD_ENABLED) {
            EmbeddedWebServer.startServer(this.directories, this.jsonStore);
        } else {
            Logging.logger().info(Messages.LOG_INTERNAL_WEB_DISABLED);
        }
    }

    private void stop() {
        if (Config.HTTPD_ENABLED) {
            EmbeddedWebServer.stopServer();
        }
        this.platform.stopCallback();
        this.worldManager.shutdown();
        if (Config.HTTPD_ENABLED && !Config.FLUSH_JSON_IMMEDIATELY) {
            this.jsonStore.flush();
        }
        this.jsonStore.clear();
    }

    public void reload(final Audience audience) {
        this.stop();

        this.configManager.reload();
        this.playerManager.reload();

        this.start();

        final Component success = Messages.PLUGIN_RELOADED.withPlaceholders(
            Components.placeholder("name", "squaremap"),
            Components.placeholder("version", this.platform.version())
        );
        audience.sendMessage(success);
    }

    public void updateCheck() {
        if (!Config.UPDATE_CHECKER) {
            return;
        }
        ForkJoinPool.commonPool().execute(() -> new UpdateChecker(Logging.logger()).checkVersion());
    }

    private void setupApi() {
        final Squaremap api = this.injector.getInstance(Squaremap.class);

        try {
            api.iconRegistry().register(SpawnIconLayer.KEY, ImageIO.read(this.directories.webDirectory().resolve("images/icon/spawn.png").toFile()));
        } catch (final IOException ex) {
            Logging.logger().warn("Failed to register spawn icon", ex);
        }

        final Method register = ReflectionUtil.needMethod(SquaremapProvider.class, List.of("register"), Squaremap.class);
        ReflectionUtil.invokeOrThrow(register, null, api);
    }

    private void shutdownApi() {
        final Method unregister = ReflectionUtil.needMethod(SquaremapProvider.class, List.of("unregister"));
        ReflectionUtil.invokeOrThrow(unregister, null);
    }

    public void shutdown() {
        this.shutdownApi();
        this.stop();
    }
}
