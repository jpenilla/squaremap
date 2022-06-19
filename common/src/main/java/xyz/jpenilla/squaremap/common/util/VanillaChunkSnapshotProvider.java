package xyz.jpenilla.squaremap.common.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.datafixers.util.Either;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
@Singleton
public final class VanillaChunkSnapshotProvider implements ChunkSnapshotProvider {
    @Inject
    private VanillaChunkSnapshotProvider() {
    }

    @Override
    public CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(
        final ServerLevel level,
        final int x,
        final int z,
        final boolean biomesOnly
    ) {
        return CompletableFuture.supplyAsync(() -> {
            final @Nullable LevelChunk chunk = fullChunkIfGenerated(level, x, z);
            if (chunk == null || chunk.isEmpty()) {
                return null;
            }
            return ChunkSnapshot.snapshot(chunk, biomesOnly);
        }, level.getServer());
    }

    private static @Nullable LevelChunk fullChunkIfGenerated(final ServerLevel level, final int x, final int z) {
        // below casts to LevelChunk are safe as full chunks should always be LevelChunks

        final ChunkPos chunkPos = new ChunkPos(x, z);
        final ChunkMapAccess chunkMap = (ChunkMapAccess) level.getChunkSource().chunkMap;

        final ChunkHolder visibleChunk = chunkMap.squaremap$getVisibleChunkIfPresent(chunkPos.toLong());
        if (visibleChunk != null) {
            final @Nullable ChunkAccess chunk = fullIfPresent(visibleChunk);
            if (chunk != null) {
                return (LevelChunk) chunk;
            }
        }

        final ChunkHolder unloadingChunk = chunkMap.squaremap$pendingUnloads().get(chunkPos.toLong());
        if (unloadingChunk != null) {
            final @Nullable ChunkAccess chunk = fullIfPresent(unloadingChunk);
            if (chunk != null) {
                return (LevelChunk) chunk;
            }
        }

        final @Nullable CompoundTag chunkTag = chunkMap.squaremap$readChunk(chunkPos).join().orElse(null);
        if (chunkTag != null && chunkTag.contains("Status", Tag.TAG_STRING)) {
            if (ChunkStatus.FULL.getName().equals(chunkTag.getString("Status"))) {
                return (LevelChunk) level.getChunkSource()
                    .getChunkFuture(x, z, ChunkStatus.FULL, true)
                    .join()
                    .left()
                    .orElse(null);
            }
        }

        return null;
    }

    private static @Nullable ChunkAccess fullIfPresent(final ChunkHolder chunkHolder) {
        final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = chunkHolder.getFutureIfPresentUnchecked(ChunkStatus.FULL);
        return future.getNow(ChunkHolder.UNLOADED_CHUNK).left().orElse(null);
    }
}
