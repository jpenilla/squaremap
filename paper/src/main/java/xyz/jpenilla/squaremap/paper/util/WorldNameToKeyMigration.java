package xyz.jpenilla.squaremap.paper.util;

import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.AbstractConfig;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public final class WorldNameToKeyMigration {
    @Inject
    private static DirectoryProvider DIRECTORY_PROVIDER;

    private WorldNameToKeyMigration() {
    }

    @SuppressWarnings("unused") // called using Reflection in AbstractWorldConfig constructor
    public static void migrate(final AbstractConfig config, final ServerLevel level) {
        final String oldName = CraftBukkitReflection.world(level).getName();
        config.migrateLevelSection(level, oldName);
        try {
            moveDirectories(level, oldName);
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to migrate directories", ex);
        }
    }

    private static void moveDirectories(final ServerLevel level, final String oldName) throws IOException {
        final String webName = Util.levelWebName(level);
        final Path tilesFrom = DIRECTORY_PROVIDER.tilesDirectory().resolve(oldName);
        if (Files.exists(tilesFrom)) {
            final Path tilesDest = DIRECTORY_PROVIDER.tilesDirectory().resolve(webName);
            Files.move(tilesFrom, tilesDest);
        }

        final Path data = DIRECTORY_PROVIDER.dataDirectory().resolve("data");
        final Path dataFrom = data.resolve(oldName);
        if (Files.exists(dataFrom)) {
            final Path dataDest = data.resolve(webName);
            Files.move(dataFrom, dataDest);
        }
    }
}
