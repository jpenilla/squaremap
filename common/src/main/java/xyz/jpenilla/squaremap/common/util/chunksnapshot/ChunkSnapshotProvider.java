package xyz.jpenilla.squaremap.common.util.chunksnapshot;

import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.ChunkPos;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface ChunkSnapshotProvider {
    CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(
        int x,
        int z,
        boolean biomesOnly
    );

    default CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(
        final ChunkPos chunkPos,
        final boolean biomesOnly
    ) {
        return this.asyncSnapshot(chunkPos.x, chunkPos.z, biomesOnly);
    }
}
