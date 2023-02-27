package xyz.jpenilla.squaremap.paper.util.chunksnapshot;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshotProvider;

@DefaultQualifier(NonNull.class)
record PaperChunkSnapshotProvider(ServerLevel level) implements ChunkSnapshotProvider {
    @Override
    public CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(
        final int x,
        final int z,
        final boolean biomesOnly
    ) {
        return CompletableFuture.supplyAsync(() -> {
            @Nullable ChunkAccess existing = this.level.getChunkIfLoadedImmediately(x, z);
            if (existing == null) {
                existing = this.level.getChunkSource().chunkMap.getUnloadingChunk(x, z);
            }
            if (existing != null && existing.getStatus().isOrAfter(ChunkStatus.FULL)) {
                return CompletableFuture.completedFuture(existing);
            }
            final CompletableFuture<@Nullable ChunkAccess> load = new CompletableFuture<>();
            this.level.getChunkSource().getChunkAtAsynchronously(x, z, ChunkStatus.EMPTY, false, false, load::complete);
            return load;
        }, this.level.getServer()).thenCompose(chunkFuture -> chunkFuture.thenApplyAsync(chunk -> {
            if (chunk == null || !chunk.getStatus().isOrAfter(ChunkStatus.FULL)) {
                return null;
            }
            if (chunk instanceof ImposterProtoChunk imposter) {
                chunk = imposter.getWrapped();
            }
            if (chunk instanceof LevelChunk levelChunk && !levelChunk.isEmpty()) {
                return ChunkSnapshot.snapshot(levelChunk, biomesOnly);
            }
            return null;
        }, this.level.getServer()));
    }
}
