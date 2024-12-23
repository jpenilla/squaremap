package xyz.jpenilla.squaremap.common.util.chunksnapshot;

import ca.spottedleaf.moonrise.common.PlatformHooks;
import ca.spottedleaf.moonrise.libs.ca.spottedleaf.concurrentutil.util.Priority;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.ChunkMapAccess;

@DefaultQualifier(NonNull.class)
record VanillaChunkSnapshotProvider(ServerLevel level, boolean moonrise) implements ChunkSnapshotProvider {
    private static final ResourceLocation FULL = BuiltInRegistries.CHUNK_STATUS.getKey(ChunkStatus.FULL);

    @Override
    public CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(final int x, final int z) {
        if (this.moonrise) {
            return this.moonriseAsyncSnapshot(x, z);
        }
        return CompletableFuture.supplyAsync(() -> {
            final @Nullable ChunkAccess chunk = chunkIfGenerated(this.level, x, z);
            if (chunk == null) {
                return null;
            }
            return ChunkSnapshot.snapshot(this.level, chunk, false);
        }, this.level.getServer());
    }

    private CompletableFuture<@Nullable ChunkSnapshot> moonriseAsyncSnapshot(final int x, final int z) {
        return CompletableFuture.supplyAsync(() -> {
            final ChunkPos chunkPos = new ChunkPos(x, z);
            final ChunkMapAccess chunkMap = (ChunkMapAccess) this.level.getChunkSource().chunkMap;

            final ChunkHolder visibleChunk = chunkMap.squaremap$getVisibleChunkIfPresent(chunkPos.toLong());
            if (visibleChunk != null) {
                final @Nullable ChunkAccess chunk = fullIfPresent(visibleChunk);
                if (chunk != null) {
                    return CompletableFuture.completedFuture(ChunkSnapshot.snapshot(this.level, chunk, false));
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
                chunk -> {
                    final @Nullable ChunkAccess unwrap = unwrap(chunk);
                    if (unwrap != null) {
                        load.complete(ChunkSnapshot.snapshot(this.level, unwrap, false));
                    } else {
                        load.complete(null);
                    }
                }
            );
            return load;
        }, this.level.getServer()).thenCompose(future -> future);
    }

    private static @Nullable ChunkAccess chunkIfGenerated(final ServerLevel level, final int x, final int z) {
        final ChunkPos chunkPos = new ChunkPos(x, z);
        final ChunkMapAccess chunkMap = (ChunkMapAccess) level.getChunkSource().chunkMap;

        final ChunkHolder visibleChunk = chunkMap.squaremap$getVisibleChunkIfPresent(chunkPos.toLong());
        if (visibleChunk != null) {
            final @Nullable ChunkAccess chunk = fullIfPresent(visibleChunk);
            if (chunk != null) {
                return chunk;
            }
        }

        final ChunkHolder unloadingChunk = chunkMap.squaremap$pendingUnloads().get(chunkPos.toLong());
        if (unloadingChunk != null) {
            final @Nullable ChunkAccess chunk = fullIfPresent(unloadingChunk);
            if (chunk != null) {
                return chunk;
            }
        }

        final @Nullable CompoundTag chunkTag = chunkMap.squaremap$readChunk(chunkPos).join().orElse(null);
        if (chunkTag != null && chunkTag.contains("Status", Tag.TAG_STRING)) {
            if (isFullStatus(chunkTag) || preHeightChangeFullChunk(chunkTag)) {
                final @Nullable ChunkAccess chunk = level.getChunkSource()
                    .getChunkFuture(x, z, ChunkStatus.EMPTY, true)
                    .join()
                    .orElse(null);
                return unwrap(chunk);
            }
        }

        return null;
    }

    private static boolean isFullStatus(final CompoundTag chunkTag) {
        return FULL.equals(ResourceLocation.tryParse(chunkTag.getString("Status")));
    }

    private static @Nullable ChunkAccess fullIfPresent(final ChunkHolder chunkHolder) {
        return unwrap(chunkHolder.getLatestChunk());
    }

    private static @Nullable ChunkAccess unwrap(@Nullable ChunkAccess chunk) {
        if (chunk == null) {
            return null;
        }
        if (chunk instanceof ImposterProtoChunk imposter) {
            chunk = imposter.getWrapped();
        }
        if (!chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL) && !preHeightChangeFullChunk(chunk)) {
            return null;
        }
        return chunk;
    }

    private static boolean preHeightChangeFullChunk(final ChunkAccess chunk) {
        return chunk.getBelowZeroRetrogen() != null && chunk.getBelowZeroRetrogen().targetStatus().isOrAfter(ChunkStatus.SPAWN);
    }

    private static boolean preHeightChangeFullChunk(final CompoundTag chunkTag) {
        final CompoundTag belowZeroRetrogen = chunkTag.getCompound("below_zero_retrogen");
        if (belowZeroRetrogen.isEmpty()) {
            return false;
        }
        final String targetStatusStr = belowZeroRetrogen.getString("target_status");
        if (targetStatusStr.isEmpty()) {
            return false;
        }
        final @Nullable ResourceLocation targetStatus = ResourceLocation.tryParse(targetStatusStr);
        if (targetStatus == null) {
            return false;
        }
        return BuiltInRegistries.CHUNK_STATUS.getValue(targetStatus).isOrAfter(ChunkStatus.SPAWN);
    }
}
