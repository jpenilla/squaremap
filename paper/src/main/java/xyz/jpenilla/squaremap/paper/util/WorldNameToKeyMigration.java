package xyz.jpenilla.squaremap.paper.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.config.Advanced;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.util.FileUtil;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public final class WorldNameToKeyMigration {
    public static void migrateLoaded() {
        for (final World world : Bukkit.getWorlds()) {
            migrate(CraftBukkitReflection.serverLevel(world));
        }
    }

    public static void migrate(final ServerLevel level) {
        final String oldName = CraftBukkitReflection.world(level).getName();
        Config.config().migrateLevelSection(level, oldName);
        Advanced.config().migrateLevelSection(level, oldName);
        try {
            moveDirectories(level, oldName);
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to migrate directories", ex);
        }
    }

    private static void moveDirectories(final ServerLevel level, final String oldName) throws IOException {
        final String webName = Util.levelWebName(level);
        final Path tilesFrom = FileUtil.TILES_DIR.resolve(oldName);
        if (Files.exists(tilesFrom)) {
            final Path tilesDest = FileUtil.TILES_DIR.resolve(webName);
            Files.move(tilesFrom, tilesDest);
        }

        final Path data = SquaremapCommon.instance().platform().dataDirectory().resolve("data");
        final Path dataFrom = data.resolve(oldName);
        if (Files.exists(dataFrom)) {
            final Path dataDest = data.resolve(webName);
            Files.move(dataFrom, dataDest);
        }
    }
}
