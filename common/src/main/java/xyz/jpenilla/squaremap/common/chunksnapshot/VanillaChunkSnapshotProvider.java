package xyz.jpenilla.squaremap.common.chunksnapshot;

import ca.spottedleaf.moonrise.common.PlatformHooks;
import ca.spottedleaf.moonrise.libs.ca.spottedleaf.concurrentutil.lock.ReentrantAreaLock;
import ca.spottedleaf.moonrise.libs.ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO;
import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.ChunkMapAccess;

@DefaultQualifier(NonNull.class)
record VanillaChunkSnapshotProvider(ServerLevel level, boolean moonrise) implements ChunkSnapshotProvider {
    private Executor mainThreadExecutor() {
        return task -> {
            if (this.level.getServer().isSameThread()) {
                task.run();
                return;
            }
            this.level.getServer().execute(task);
        };
    }

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
            return ChunkSnapshotFactory.snapshotLiveChunk(this.level, chunk);
        }, this.mainThreadExecutor());
    }

    private CompletableFuture<@Nullable ChunkSnapshot> moonriseAsyncSnapshot(final int x, final int z) {
        final ChunkTaskScheduler scheduler = ((ChunkSystemServerLevel) this.level).moonrise$getChunkTaskScheduler();
        final @Nullable CompletableFuture<@Nullable CompoundTag> data;
        final ReentrantAreaLock.Node lock = scheduler.schedulingLockArea.lock(x, z);
        try {
            // Keep holder creation/unloading from interleaving with disk-read registration.
            data = scheduler.chunkHolderManager.getChunkHolder(x, z) == null ? this.moonriseReadChunkData(x, z) : null;
        } finally {
            scheduler.schedulingLockArea.unlock(lock);
        }
        if (data != null) {
            return this.moonriseReadSnapshot(x, z, data);
        }

        return CompletableFuture.supplyAsync(() -> {
            // Recheck on main; never carry live chunk state across the executor hop.
            final NewChunkHolder chunkHolder = scheduler.chunkHolderManager.getChunkHolder(x, z);

            if (chunkHolder != null) {
                final @Nullable ChunkAccess chunk = chunkHolder.getChunkIfPresent(ChunkStatus.FULL);
                if (chunk != null && ChunkSnapshotEligibility.get(chunk.getPersistedStatus(), chunk.getBelowZeroRetrogen()) == ChunkSnapshotEligibility.ELIGIBLE) {
                    return CompletableFuture.completedFuture(ChunkSnapshotFactory.snapshotLiveChunk(this.level, chunk));
                }
            }

            return this.moonriseChunkSystemSnapshot(x, z);
        }, this.mainThreadExecutor()).thenCompose(future -> future);
    }

    private CompletableFuture<@Nullable ChunkSnapshot> moonriseReadSnapshot(int x, int z, final CompletableFuture<@Nullable CompoundTag> data) {
        final ChunkMapAccess chunkMapAccess = (ChunkMapAccess) this.level.getChunkSource().chunkMap;
        final ChunkTaskScheduler scheduler = ((ChunkSystemServerLevel) this.level).moonrise$getChunkTaskScheduler();
        final Executor executor = task -> scheduler.loadExecutor.createTask(task, Priority.NORMAL).queue();
        final CompletableFuture<@Nullable CompoundTag> upgraded = data.thenApplyAsync(
            tag -> tag == null ? null : chunkMapAccess.squaremap$upgradeChunkTag(tag),
            executor
        );
        final LevelHeightAccessor heightAccessor = LevelHeightAccessor.create(this.level.getMinY(), this.level.getHeight());
        final PalettedContainerFactory palettedContainerFactory = this.level.palettedContainerFactory();
        final DimensionType dimensionType = this.level.dimensionType();
        return upgraded.thenComposeAsync(
            tag -> {
                if (tag == null) {
                    return CompletableFuture.completedFuture(null);
                }
                final ChunkStatus status = tag.read(ChunkDataKeys.STATUS, ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY);
                final @Nullable BelowZeroRetrogen retroGen = tag.read(ChunkDataKeys.RETROGEN, BelowZeroRetrogen.CODEC).orElse(null);
                return switch (ChunkSnapshotEligibility.get(status, retroGen)) {
                    case INELIGIBLE -> CompletableFuture.completedFuture(null);
                    case ELIGIBLE -> {
                        if (ChunkSnapshotFactory.chunkPosMatches(tag, x, z)) {
                            yield CompletableFuture.completedFuture(ChunkSnapshotFactory.snapshotFromChunkData(heightAccessor, dimensionType, palettedContainerFactory, tag));
                        } else {
                            yield this.moonriseChunkSystemSnapshot(x, z);
                        }
                    }
                    case NEEDS_RETROGEN -> this.moonriseChunkSystemSnapshot(x, z);
                };
            },
            executor
        );
    }

    private CompletableFuture<@Nullable ChunkSnapshot> moonriseChunkSystemSnapshot(final int x, final int z) {
        return CompletableFuture.supplyAsync(() -> {
            final CompletableFuture<@Nullable ChunkSnapshot> load = new CompletableFuture<>();
            PlatformHooks.get().scheduleChunkLoad(
                this.level,
                x,
                z,
                ChunkStatus.EMPTY,
                true,
                Priority.NORMAL,
                chunk -> {
                    try {
                        final @Nullable ChunkAccess unwrapped = unwrap(chunk);
                        if (unwrapped == null) {
                            load.complete(null);
                        } else {
                            switch (ChunkSnapshotEligibility.get(unwrapped.getPersistedStatus(), unwrapped.getBelowZeroRetrogen())) {
                                case INELIGIBLE -> load.complete(null);
                                case ELIGIBLE -> load.complete(ChunkSnapshotFactory.snapshotLiveChunk(this.level, unwrapped));
                                case NEEDS_RETROGEN -> {
                                    // EMPTY only reads the chunk. Finish eligible upgrades before snapshotting;
                                    // ordinary incomplete chunks never request FULL.
                                    PlatformHooks.get().scheduleChunkLoad(
                                        this.level, x, z, ChunkStatus.FULL, true, Priority.NORMAL,
                                        completed -> this.completeSnapshot(load, completed)
                                    );
                                }
                            }
                        }
                    } catch (final Throwable error) {
                        load.completeExceptionally(error);
                    }
                }
            );
            return load;
        }, this.mainThreadExecutor()).thenCompose(future -> future);
    }

    private void completeSnapshot(final CompletableFuture<@Nullable ChunkSnapshot> result, final @Nullable ChunkAccess chunk) {
        try {
            final @Nullable ChunkAccess completed = unwrap(chunk);
            if (completed == null) {
                result.complete(null);
            } else if (ChunkSnapshotEligibility.get(completed.getPersistedStatus(), completed.getBelowZeroRetrogen()) != ChunkSnapshotEligibility.ELIGIBLE) {
                result.completeExceptionally(new IllegalStateException("Chunk upgrade did not finish: " + completed.getPos()));
            } else {
                result.complete(ChunkSnapshotFactory.snapshotLiveChunk(this.level, completed));
            }
        } catch (final Throwable error) {
            result.completeExceptionally(error);
        }
    }

    private CompletableFuture<@Nullable CompoundTag> moonriseReadChunkData(int x, int z) {
        final CompletableFuture<@Nullable CompoundTag> data = new CompletableFuture<>();

        MoonriseRegionFileIO.loadDataAsync(
            this.level,
            x,
            z,
            MoonriseRegionFileIO.RegionFileType.CHUNK_DATA,
            (tag, error) -> {
                if (error != null) {
                    data.completeExceptionally(error);
                } else {
                    data.complete(tag);
                }
            },
            false, // not intending to block
            Priority.NORMAL
        );

        return data;
    }

    private static @Nullable ChunkAccess chunkIfGenerated(final ServerLevel level, final int x, final int z) {
        final ChunkPos chunkPos = new ChunkPos(x, z);
        final ChunkMapAccess chunkMap = (ChunkMapAccess) level.getChunkSource().chunkMap;

        final ChunkHolder visibleChunk = chunkMap.squaremap$getVisibleChunkIfPresent(chunkPos.pack());
        if (visibleChunk != null) {
            final @Nullable ChunkAccess chunk = unwrap(visibleChunk.getLatestChunk());
            if (chunk != null) {
                switch (ChunkSnapshotEligibility.get(chunk.getPersistedStatus(), chunk.getBelowZeroRetrogen())) {
                    case ELIGIBLE -> {
                        return chunk;
                    }
                    case NEEDS_RETROGEN -> {
                        return vanillaFinishRetrogen(level, x, z);
                    }
                    case INELIGIBLE -> {
                        // Continue lookup
                    }
                }
            }
        }

        final ChunkHolder unloadingChunk = chunkMap.squaremap$pendingUnloads().get(chunkPos.pack());
        if (unloadingChunk != null) {
            final @Nullable ChunkAccess chunk = unwrap(unloadingChunk.getLatestChunk());
            if (chunk != null) {
                switch (ChunkSnapshotEligibility.get(chunk.getPersistedStatus(), chunk.getBelowZeroRetrogen())) {
                    case ELIGIBLE -> {
                        return chunk;
                    }
                    case NEEDS_RETROGEN -> {
                        return vanillaFinishRetrogen(level, x, z);
                    }
                    case INELIGIBLE -> {
                        // Continue lookup
                    }
                }
            }
        }

        final @Nullable CompoundTag chunkTag = chunkMap.squaremap$readChunk(chunkPos).join().orElse(null);
        if (chunkTag != null) {
            final ChunkStatus status = chunkTag.read(ChunkDataKeys.STATUS, ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY);
            final @Nullable BelowZeroRetrogen retroGen = chunkTag.read(ChunkDataKeys.RETROGEN, BelowZeroRetrogen.CODEC).orElse(null);
            if (ChunkSnapshotEligibility.get(status, retroGen) != ChunkSnapshotEligibility.INELIGIBLE) {
                final @Nullable ChunkAccess chunk = level.getChunkSource()
                    .getChunkFuture(x, z, ChunkStatus.EMPTY, true)
                    .join()
                    .orElse(null);
                final @Nullable ChunkAccess unwrapped = unwrap(chunk);
                if (unwrapped != null) {
                    return switch (ChunkSnapshotEligibility.get(unwrapped.getPersistedStatus(), unwrapped.getBelowZeroRetrogen())) {
                        case INELIGIBLE -> null;
                        case ELIGIBLE -> unwrapped;
                        case NEEDS_RETROGEN -> vanillaFinishRetrogen(level, x, z);
                    };
                }
            }
        }

        return null;
    }

    private static @Nullable ChunkAccess vanillaFinishRetrogen(final ServerLevel level, final int x, final int z) {
        final @Nullable ChunkAccess completed = unwrap(level.getChunkSource()
            .getChunkFuture(x, z, ChunkStatus.FULL, true).join().orElse(null));
        if (completed != null && ChunkSnapshotEligibility.get(completed.getPersistedStatus(), completed.getBelowZeroRetrogen()) != ChunkSnapshotEligibility.ELIGIBLE) {
            throw new IllegalStateException("Chunk upgrade did not finish: " + completed.getPos());
        }
        return completed;
    }

    private static @Nullable ChunkAccess unwrap(@Nullable ChunkAccess chunk) {
        if (chunk == null) {
            return null;
        }
        if (chunk instanceof ImposterProtoChunk imposter) {
            chunk = imposter.getWrapped();
        }
        return chunk;
    }
}
