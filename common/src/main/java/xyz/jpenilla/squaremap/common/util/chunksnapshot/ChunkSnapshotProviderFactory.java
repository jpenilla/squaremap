package xyz.jpenilla.squaremap.common.util.chunksnapshot;

import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface ChunkSnapshotProviderFactory {
    ChunkSnapshotProvider createChunkSnapshotProvider(ServerLevel level);
}
