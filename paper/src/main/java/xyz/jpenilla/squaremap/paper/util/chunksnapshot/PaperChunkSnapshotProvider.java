package xyz.jpenilla.squaremap.paper.util.chunksnapshot;

import ca.spottedleaf.concurrentutil.executor.standard.PrioritisedExecutor;
import ca.spottedleaf.moonrise.patches.chunk_system.ChunkSystem;
import io.papermc.paper.util.TickThread;
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
            if (existing != null && existing.getPersistedStatus().isOrAfter(ChunkStatus.FULL)) {
                return CompletableFuture.completedFuture(existing);
            } else if (existing != null) {
                return CompletableFuture.<@Nullable ChunkAccess>completedFuture(null);
            }
            final CompletableFuture<@Nullable ChunkAccess> load = new CompletableFuture<>();
            ChunkSystem.scheduleChunkLoad(
                this.level,
                x,
                z,
                ChunkStatus.EMPTY,
                true,
                PrioritisedExecutor.Priority.NORMAL,
                load::complete
            );
            return load;
        }, this.executor(x, z)).thenCompose(chunkFuture -> chunkFuture.thenApplyAsync(chunk -> {
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
        }, this.executor(x, z)));
    }

    private Executor executor(final int x, final int z) {
        if (!Folia.FOLIA) {
            return this.level.getServer();
        }
        return task -> {
            if (TickThread.isTickThreadFor(this.level, x, z)) {
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
