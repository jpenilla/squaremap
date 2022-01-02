package xyz.jpenilla.squaremap.common;

import cloud.commandframework.CommandManager;
import java.nio.file.Path;
import java.util.Collection;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.minecraft.server.level.ServerLevel;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.PlayerManager;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.util.BiomeSpecialEffectsAccess;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;

@DefaultQualifier(NonNull.class)
public interface SquaremapPlatform {
    ChunkSnapshotProvider chunkSnapshotProvider();

    WorldManager worldManager();

    Path dataDirectory();

    Logger logger();

    ComponentFlattener componentFlattener();

    String configNameForWorld(ServerLevel level);

    String tilesDirNameForWorld(ServerLevel level);

    Collection<ServerLevel> levels();

    @Nullable ServerLevel level(WorldIdentifier identifier);

    Path regionFileDirectory(ServerLevel level);

    BiomeSpecialEffectsAccess biomeSpecialEffectsAccess();

    PlayerManager playerManager();

    void startCallback();

    void stopCallback();

    CommandManager<Commander> createCommandManager();
}
