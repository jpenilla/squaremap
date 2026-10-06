package xyz.jpenilla.squaremap.paper.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.SquaremapDirectories;
import xyz.jpenilla.squaremap.common.config.AbstractConfig;
import xyz.jpenilla.squaremap.common.world.WorldIdentifiers;

@DefaultQualifier(NonNull.class)
public final class WorldNameToKeyMigration {
    private WorldNameToKeyMigration() {
    }

    @SuppressWarnings("unused") // called using Reflection in AbstractWorldConfig constructor
    public static void migrate(final AbstractConfig config, final ServerLevel level) {
        final String oldName = level.getWorld().getName();
        config.migrateLevelSection(level, oldName);
    }

    public static void tryMoveDirectories(final SquaremapDirectories directories, final ServerLevel level) {
        try {
            moveDirectories(directories, level);
        } catch (final IOException ex) {
            Logging.logger().error("Failed to migrate directories for '{}'", level.dimension().identifier());
        }
    }

    private static void moveDirectories(final SquaremapDirectories directories, final ServerLevel level) throws IOException {
        final String oldName = level.getWorld().getName();
        final String webName = WorldIdentifiers.webName(level);
        final Path tilesFrom = directories.tilesDirectory().resolve(oldName);
        if (Files.exists(tilesFrom)) {
            final Path tilesDest = directories.tilesDirectory().resolve(webName);
            Files.move(tilesFrom, tilesDest);
        }

        final Path data = directories.dataDirectory().resolve("data");
        final Path dataFrom = data.resolve(oldName);
        if (Files.exists(dataFrom)) {
            final Path dataDest = data.resolve(webName);
            Files.move(dataFrom, dataDest);
        }
    }
}
