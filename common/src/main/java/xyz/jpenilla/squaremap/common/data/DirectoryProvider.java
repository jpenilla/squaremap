package xyz.jpenilla.squaremap.common.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.inject.annotation.DataDirectory;
import xyz.jpenilla.squaremap.common.util.RegionFileDirectoryResolver;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
@Singleton
public final class DirectoryProvider {
    private final RegionFileDirectoryResolver regionFileDirectoryResolver;
    private final Path dataDirectory;
    private final Path webDirectory;
    private final Path tilesDirectory;
    private final Path localeDirectory;

    @Inject
    private DirectoryProvider(
        final RegionFileDirectoryResolver regionFileDirectoryResolver,
        @DataDirectory final Path dataDirectory
    ) {
        this.regionFileDirectoryResolver = regionFileDirectoryResolver;
        this.dataDirectory = dataDirectory;
        this.webDirectory = dataDirectory.resolve(Config.WEB_DIR);
        this.tilesDirectory = this.webDirectory.resolve("tiles");
        this.localeDirectory = dataDirectory.resolve("locale");
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

    public Path[] getRegionFiles(final ServerLevel level) {
        final Path regionFolder = this.regionFileDirectoryResolver.resolveRegionFileDirectory(level);
        Logging.debug(() -> "Listing region files for directory '" + regionFolder + "'...");
        try (final Stream<Path> stream = Files.list(regionFolder)) {
            return stream.filter(file -> file.getFileName().toString().endsWith(".mca")).toArray(Path[]::new);
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to list region files in directory '" + regionFolder.toAbsolutePath() + "'", ex);
        }
    }
}
