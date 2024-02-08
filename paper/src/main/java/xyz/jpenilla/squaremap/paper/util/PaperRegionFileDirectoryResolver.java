package xyz.jpenilla.squaremap.paper.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.RegionFileDirectoryResolver;

@DefaultQualifier(NonNull.class)
@Singleton
public final class PaperRegionFileDirectoryResolver implements RegionFileDirectoryResolver {
    @Inject
    private PaperRegionFileDirectoryResolver() {
    }

    @Override
    public Path resolveRegionFileDirectory(final ServerLevel level) {
        final Path storageFolder = LevelStorageSource.getStorageFolder(
            CraftBukkitReflection.world(level).getWorldFolder().toPath(),
            level.getTypeKey()
        );
        final Path sector = storageFolder.resolve("sectors");
        if (Files.exists(sector)) {
            return sector;
        }
        return storageFolder.resolve("region");
    }
}
