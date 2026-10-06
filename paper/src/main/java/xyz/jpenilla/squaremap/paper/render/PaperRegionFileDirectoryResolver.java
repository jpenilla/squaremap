package xyz.jpenilla.squaremap.paper.render;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.render.RegionFileDirectoryResolver;

@DefaultQualifier(NonNull.class)
@Singleton
public final class PaperRegionFileDirectoryResolver implements RegionFileDirectoryResolver {
    @Inject
    private PaperRegionFileDirectoryResolver() {
    }

    @Override
    public Path resolveRegionFileDirectory(final ServerLevel level) {
        return level.getWorld().getWorldPath().resolve("region");
    }
}
