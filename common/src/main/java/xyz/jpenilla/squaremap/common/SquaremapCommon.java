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
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.data.LevelBiomeColorData;
import xyz.jpenilla.squaremap.common.httpd.IntegratedServer;
import xyz.jpenilla.squaremap.common.layer.SpawnIconProvider;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;
import xyz.jpenilla.squaremap.common.util.UpdateChecker;

import static xyz.jpenilla.squaremap.common.util.Components.miniMessage;

@DefaultQualifier(NonNull.class)
@Singleton
public final class SquaremapCommon {
    private final Injector injector;
    private final SquaremapPlatform platform;
    private final DirectoryProvider directoryProvider;
    private final ConfigManager configManager;
    private @MonotonicNonNull Squaremap api;

    @Inject
    private SquaremapCommon(
        final Injector injector,
        final SquaremapPlatform platform,
        final DirectoryProvider directoryProvider,
        final ConfigManager configManager
    ) {
        this.injector = injector;
        this.platform = platform;
        this.directoryProvider = directoryProvider;
        this.configManager = configManager;
    }

    public void init() {
        this.configManager.init();
        this.start();
        this.setupApi();
        this.injector.getInstance(Commands.class).registerCommands();
    }

    public void start() {
        LevelBiomeColorData.loadImages(this.directoryProvider);
        this.platform.startCallback();
        if (Config.HTTPD_ENABLED) {
            IntegratedServer.startServer(this.directoryProvider);
        } else {
            Logging.logger().info(Lang.LOG_INTERNAL_WEB_DISABLED);
        }
    }

    public void stop() {
        if (Config.HTTPD_ENABLED) {
            IntegratedServer.stopServer();
        }
        this.platform.stopCallback();
    }

    public void reload(final Audience audience) {
        this.stop();

        this.configManager.reload();

        this.start();

        final Component success = miniMessage(
            Lang.PLUGIN_RELOADED,
            Placeholder.unparsed("name", "squaremap"),
            Placeholder.unparsed("version", this.platform.version())
        );
        audience.sendMessage(success);
    }

    public void updateCheck() {
        if (!Config.UPDATE_CHECKER) {
            return;
        }
        ForkJoinPool.commonPool().execute(() -> new UpdateChecker(Logging.logger(), "jpenilla/squaremap").checkVersion());
    }

    public void setupApi() {
        this.api = this.injector.getInstance(Squaremap.class);

        try {
            this.api.iconRegistry().register(SpawnIconProvider.SPAWN_ICON_KEY, ImageIO.read(this.directoryProvider.webDirectory().resolve("images/icon/spawn.png").toFile()));
        } catch (final IOException e) {
            Logging.logger().warn("Failed to register spawn icon", e);
        }

        final Method register = ReflectionUtil.needMethod(SquaremapProvider.class, List.of("register"), Squaremap.class);
        ReflectionUtil.invokeOrThrow(register, null, this.api);
    }

    public void shutdown() {
        this.shutdownApi();
        this.stop();
    }

    public void shutdownApi() {
        final Method unregister = ReflectionUtil.needMethod(SquaremapProvider.class, List.of("unregister"));
        ReflectionUtil.invokeOrThrow(unregister, null);
        this.api = null;
    }

    public Squaremap api() {
        return this.api;
    }
}
