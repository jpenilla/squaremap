package xyz.jpenilla.squaremap.paper.util.chunksnapshot;

import ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.common.PlatformHooks;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.paper.util.Folia;

@DefaultQualifier(NonNull.class)
record PaperChunkSnapshotProvider(
    ServerLevel level,
    Server server,
    JavaPlugin plugin
) implements ChunkSnapshotProvider {
    @Override
    public CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(final int x, final int z) {
        return CompletableFuture.supplyAsync(() -> {
            final @Nullable ChunkAccess existing = this.level.getChunkIfLoadedImmediately(x, z);
            if (existing != null) {
                final @Nullable ChunkSnapshot snapshot = this.maybeSnapshot(existing);
                if (snapshot != null) {
                    return CompletableFuture.completedFuture(snapshot);
                }
            }
            final CompletableFuture<@Nullable ChunkSnapshot> load = new CompletableFuture<>();
            PlatformHooks.get().scheduleChunkLoad(
                this.level,
                x,
                z,
                ChunkStatus.EMPTY,
                true,
                Priority.NORMAL,
                chunk -> load.complete(this.maybeSnapshot(chunk))
            );
            return load;
        }, this.executor(x, z)).thenCompose(future -> future);
    }

    private @Nullable ChunkSnapshot maybeSnapshot(@Nullable ChunkAccess chunk) {
        if (chunk == null) {
            return null;
        }
        if (chunk instanceof ImposterProtoChunk imposter) {
            chunk = imposter.getWrapped();
        }
        if (!chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
            if (chunk.getBelowZeroRetrogen() == null || !chunk.getBelowZeroRetrogen().targetStatus().isOrAfter(ChunkStatus.SPAWN)) {
                return null;
            }
        }
        return ChunkSnapshot.snapshot(this.level, chunk, false);
    }

    private Executor executor(final int x, final int z) {
        if (!Folia.FOLIA) {
            return this.level.getServer();
        }
        return task -> {
            if (this.server.isOwnedByCurrentRegion(this.level.getWorld(), x, z)) {
                task.run();
                return;
            }
            this.server.getRegionScheduler().execute(
                this.plugin,
                this.level.getWorld(),
                x,
                z,
                task
            );
        };
    }
}
