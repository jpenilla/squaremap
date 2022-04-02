package xyz.jpenilla.squaremap.common.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.inject.annotation.DataDirectory;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
@Singleton
public final class DirectoryProvider {
    private final Path dataDirectory;
    private final Path localeDirectory;
    private @MonotonicNonNull Path webDirectory;
    private @MonotonicNonNull Path tilesDirectory;

    @Inject
    private DirectoryProvider(@DataDirectory final Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.localeDirectory = dataDirectory.resolve("locale");
    }

    public void init() {
        this.webDirectory = this.dataDirectory.resolve(Config.WEB_DIR);
        this.tilesDirectory = this.webDirectory.resolve("tiles");
    }

    public Path dataDirectory() {
        return this.dataDirectory;
    }

    public Path webDirectory() {
        return this.webDirectory;
    }

    public Path tilesDirectory() {
        return this.tilesDirectory;
    }

    public Path localeDirectory() {
        return this.localeDirectory;
    }

    public Path getAndCreateTilesDirectory(final ServerLevel level) {
        final Path dir = this.tilesDirectory.resolve(Util.levelWebName(level));
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (final IOException e) {
                Logging.error(Lang.LOG_COULD_NOT_CREATE_DIR, e, "path", dir.toAbsolutePath());
            }
        }
        return dir;
    }
}
