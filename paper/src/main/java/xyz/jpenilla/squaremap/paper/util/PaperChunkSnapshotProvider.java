package xyz.jpenilla.squaremap.paper.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;

@DefaultQualifier(NonNull.class)
@Singleton
public final class PaperChunkSnapshotProvider implements ChunkSnapshotProvider {
    @Inject
    private PaperChunkSnapshotProvider() {
    }

    @Override
    public CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(
        final ServerLevel level,
        final int x,
        final int z,
        final boolean biomesOnly
    ) {
        return CompletableFuture.supplyAsync(() -> {
            @Nullable ChunkAccess existing = level.getChunkIfLoadedImmediately(x, z);
            if (existing == null) {
                existing = level.getChunkSource().chunkMap.getUnloadingChunk(x, z);
            }
            if (existing != null && existing.getStatus().isOrAfter(ChunkStatus.FULL)) {
                return CompletableFuture.completedFuture(existing);
            }
            final CompletableFuture<@Nullable ChunkAccess> load = new CompletableFuture<>();
            level.getChunkSource().getChunkAtAsynchronously(x, z, ChunkStatus.EMPTY, false, false, load::complete);
            return load;
        }, level.getServer()).thenCompose(chunkFuture -> chunkFuture.thenApplyAsync(chunk -> {
            if (chunk == null || !chunk.getStatus().isOrAfter(ChunkStatus.FULL)) {
                return null;
            }
            if (chunk instanceof ImposterProtoChunk imposter) {
                chunk = imposter.getWrapped();
            }
            return ChunkSnapshot.snapshot((LevelChunk) chunk, biomesOnly);
        }, level.getServer()));
    }
}
