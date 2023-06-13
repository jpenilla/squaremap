package xyz.jpenilla.squaremap.common.util.chunksnapshot;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.ChunkMapAccess;

@DefaultQualifier(NonNull.class)
record VanillaChunkSnapshotProvider(ServerLevel level) implements ChunkSnapshotProvider {
    private static final ResourceLocation FULL = BuiltInRegistries.CHUNK_STATUS.getKey(ChunkStatus.FULL);

    @Override
    public CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(
        final int x,
        final int z,
        final boolean biomesOnly
    ) {
        return CompletableFuture.supplyAsync(() -> {
            final @Nullable LevelChunk chunk = fullChunkIfGenerated(this.level, x, z);
            if (chunk == null || chunk.isEmpty()) {
                return null;
            }
            return ChunkSnapshot.snapshot(chunk, biomesOnly);
        }, this.level.getServer());
    }

    private static @Nullable LevelChunk fullChunkIfGenerated(final ServerLevel level, final int x, final int z) {
        final ChunkPos chunkPos = new ChunkPos(x, z);
        final ChunkMapAccess chunkMap = (ChunkMapAccess) level.getChunkSource().chunkMap;

        final ChunkHolder visibleChunk = chunkMap.squaremap$getVisibleChunkIfPresent(chunkPos.toLong());
        if (visibleChunk != null) {
            final @Nullable LevelChunk chunk = fullIfPresent(visibleChunk);
            if (chunk != null) {
                return chunk;
            }
        }

        final ChunkHolder unloadingChunk = chunkMap.squaremap$pendingUnloads().get(chunkPos.toLong());
        if (unloadingChunk != null) {
            final @Nullable LevelChunk chunk = fullIfPresent(unloadingChunk);
            if (chunk != null) {
                return chunk;
            }
        }

        final @Nullable CompoundTag chunkTag = chunkMap.squaremap$readChunk(chunkPos).join().orElse(null);
        if (chunkTag != null && chunkTag.contains("Status", Tag.TAG_STRING)) {
            if (isFullStatus(chunkTag)) {
                @Nullable ChunkAccess chunk = level.getChunkSource()
                    .getChunkFuture(x, z, ChunkStatus.EMPTY, true)
                    .join()
                    .left()
                    .orElse(null);
                return unwrap(chunk);
            }
        }

        return null;
    }

    private static boolean isFullStatus(final CompoundTag chunkTag) {
        return FULL.equals(ResourceLocation.tryParse(chunkTag.getString("Status")));
    }

    private static @Nullable LevelChunk fullIfPresent(final ChunkHolder chunkHolder) {
        return unwrap(chunkHolder.getLastAvailable());
    }

    private static @Nullable LevelChunk unwrap(@Nullable ChunkAccess chunk) {
        if (chunk == null || !chunk.getStatus().isOrAfter(ChunkStatus.FULL)) {
            return null;
        }
        if (chunk instanceof ImposterProtoChunk imposter) {
            chunk = imposter.getWrapped();
        }
        return (LevelChunk) chunk;
    }
}
