package xyz.jpenilla.squaremap.paper.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.AbstractConfig;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public final class WorldNameToKeyMigration {
    private WorldNameToKeyMigration() {
    }

    @SuppressWarnings("unused") // called using Reflection in AbstractWorldConfig constructor
    public static void migrate(final AbstractConfig config, final ServerLevel level) {
        final String oldName = CraftBukkitReflection.world(level).getName();
        config.migrateLevelSection(level, oldName);
    }

    public static void tryMoveDirectories(final DirectoryProvider directoryProvider, final ServerLevel level) {
        try {
            moveDirectories(directoryProvider, level);
        } catch (final IOException ex) {
            Logging.logger().error("Failed to migrate directories for '{}'", level.dimension().location());
        }
    }

    private static void moveDirectories(final DirectoryProvider directoryProvider, final ServerLevel level) throws IOException {
        final String oldName = CraftBukkitReflection.world(level).getName();
        final String webName = Util.levelWebName(level);
        final Path tilesFrom = directoryProvider.tilesDirectory().resolve(oldName);
        if (Files.exists(tilesFrom)) {
            final Path tilesDest = directoryProvider.tilesDirectory().resolve(webName);
            Files.move(tilesFrom, tilesDest);
        }

        final Path data = directoryProvider.dataDirectory().resolve("data");
        final Path dataFrom = data.resolve(oldName);
        if (Files.exists(dataFrom)) {
            final Path dataDest = data.resolve(webName);
            Files.move(dataFrom, dataDest);
        }
    }
}
