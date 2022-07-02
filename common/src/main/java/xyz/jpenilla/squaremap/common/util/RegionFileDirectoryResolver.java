package xyz.jpenilla.squaremap.common.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface RegionFileDirectoryResolver {
    Path resolveRegionFileDirectory(ServerLevel level);

    @Singleton
    class Vanilla implements RegionFileDirectoryResolver {
        @Inject
        private Vanilla() {
        }

        @Override
        public Path resolveRegionFileDirectory(final ServerLevel level) {
            final Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);
            return DimensionType.getStorageFolder(level.dimension(), worldPath).resolve("region");
        }
    }
}
