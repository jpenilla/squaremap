package xyz.jpenilla.squaremap.common;

import com.google.inject.Injector;
import java.nio.file.Path;
import java.util.Collection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

@DefaultQualifier(NonNull.class)
public interface SquaremapPlatform {
    Injector injector();

    void startCallback();

    void stopCallback();

    Logger logger();

    Path dataDirectory();

    WorldManager worldManager();

    AbstractPlayerManager playerManager();

    Collection<ServerLevel> levels();

    @Nullable ServerLevel level(WorldIdentifier identifier);

    int maxPlayers();

    String version();

    default Path regionFileDirectory(final ServerLevel level) {
        final Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);
        return DimensionType.getStorageFolder(level.dimension(), worldPath).resolve("region");
    }
}
