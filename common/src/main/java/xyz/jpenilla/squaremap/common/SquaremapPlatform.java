package xyz.jpenilla.squaremap.common;

import cloud.commandframework.CommandManager;
import java.nio.file.Path;
import java.util.Collection;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;

@DefaultQualifier(NonNull.class)
public interface SquaremapPlatform {
    ChunkSnapshotProvider chunkSnapshotProvider();

    WorldManager worldManager();

    Path dataDirectory();

    Logger logger();

    ComponentFlattener componentFlattener();

    default String configNameForWorld(final ServerLevel level) {
        return level.dimension().location().toString();
    }

    default String webNameForWorld(final ServerLevel level) {
        return level.dimension().location().toString().replace(":", "_");
    }

    Collection<ServerLevel> levels();

    @Nullable ServerLevel level(WorldIdentifier identifier);

    default Path regionFileDirectory(final ServerLevel level) {
        final Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);
        return DimensionType.getStorageFolder(level.dimension(), worldPath).resolve("region");
    }

    PlayerManagerImpl playerManager();

    void startCallback();

    void stopCallback();

    int maxPlayers();

    String version();

    CommandManager<Commander> createCommandManager();
}
