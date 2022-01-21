package xyz.jpenilla.squaremap.paper.util;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;

@DefaultQualifier(NonNull.class)
public final class PaperChunkSnapshotProvider implements ChunkSnapshotProvider {
    private static final PaperChunkSnapshotProvider INSTANCE = new PaperChunkSnapshotProvider();

    private PaperChunkSnapshotProvider() {
    }

    @Override
    public CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(
        final ServerLevel level,
        final int x,
        final int z,
        final boolean biomesOnly
    ) {
        final Supplier<CompletableFuture<@Nullable ChunkSnapshot>> futureSupplier = () -> level.getChunkSource()
            .getChunkAtAsynchronously(x, z, false, false)
            .thenApply(either -> {
                final @Nullable LevelChunk chunk = (LevelChunk) either.left().orElse(null);
                if (chunk == null || chunk.isEmpty()) {
                    return null;
                }
                return ChunkSnapshot.snapshot(chunk, biomesOnly);
            });
        return CompletableFuture.supplyAsync(futureSupplier, level.getServer()).thenCompose(Function.identity());
    }

    public static PaperChunkSnapshotProvider get() {
        return INSTANCE;
    }
}
