package xyz.jpenilla.squaremap.common;

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
import xyz.jpenilla.squaremap.common.config.Advanced;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.httpd.IntegratedServer;
import xyz.jpenilla.squaremap.common.layer.SpawnIconProvider;
import xyz.jpenilla.squaremap.common.util.FileUtil;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;
import xyz.jpenilla.squaremap.common.util.UpdateChecker;

import static xyz.jpenilla.squaremap.common.util.Components.miniMessage;

@DefaultQualifier(NonNull.class)
public final class SquaremapCommon {
    private static @MonotonicNonNull SquaremapCommon INSTANCE;

    private final SquaremapPlatform platform;
    private final Commands commands;
    private @MonotonicNonNull SquaremapApiProvider squaremap;

    public SquaremapCommon(final SquaremapPlatform platform) {
        INSTANCE = this;
        this.platform = platform;

        Config.reload();

        // this has to load after config.yml in order to know if web dir should be overwritten
        // but also before advanced.yml to ensure foliage.png and grass.png are already on disk
        FileUtil.extract("/web/", FileUtil.WEB_DIR.toFile(), Config.UPDATE_WEB_DIR);
        FileUtil.extract("/locale/", FileUtil.LOCALE_DIR.toFile(), false);

        Advanced.reload();
        Lang.reload();

        this.start();
        this.setupApi();

        this.commands = new Commands(this);
    }

    public void start() {
        this.platform.startCallback();
        if (Config.HTTPD_ENABLED) {
            IntegratedServer.startServer();
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

        Config.reload();
        Advanced.reload();
        Lang.reload();
        FileUtil.reload();

        this.start();

        final Component success = miniMessage(
            Lang.PLUGIN_RELOADED,
            Placeholder.unparsed("name", "squaremap"),
            Placeholder.unparsed("version", this.platform().version())
        );
        audience.sendMessage(success);
    }

    public void updateCheck() {
        if (!Config.UPDATE_CHECKER) {
            return;
        }
        ForkJoinPool.commonPool().execute(() -> new UpdateChecker(this.platform.logger(), "jpenilla/squaremap").checkVersion());
    }

    public void setupApi() {
        this.squaremap = new SquaremapApiProvider(this.platform);

        try {
            this.squaremap.iconRegistry().register(SpawnIconProvider.SPAWN_ICON_KEY, ImageIO.read(FileUtil.WEB_DIR.resolve("images/icon/spawn.png").toFile()));
        } catch (final IOException e) {
            Logging.logger().warn("Failed to register spawn icon", e);
        }

        final Method register = ReflectionUtil.needMethod(SquaremapProvider.class, List.of("register"), Squaremap.class);
        ReflectionUtil.invokeOrThrow(register, null, this.squaremap);
    }

    public void shutdown() {
        this.shutdownApi();
        this.stop();
    }

    public void shutdownApi() {
        final Method unregister = ReflectionUtil.needMethod(SquaremapProvider.class, List.of("unregister"));
        ReflectionUtil.invokeOrThrow(unregister, null);
        this.squaremap = null;
    }

    public @NonNull Squaremap api() {
        return this.squaremap;
    }

    public SquaremapPlatform platform() {
        return this.platform;
    }

    public Commands commands() {
        return this.commands;
    }

    public static SquaremapCommon instance() {
        return INSTANCE;
    }
}
