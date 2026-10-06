package xyz.jpenilla.squaremap.paper.chunksnapshot;

import ca.spottedleaf.concurrentutil.lock.ReentrantAreaLock;
import ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.common.PlatformHooks;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.chunksnapshot.ChunkDataKeys;
import xyz.jpenilla.squaremap.common.chunksnapshot.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.chunksnapshot.ChunkSnapshotEligibility;
import xyz.jpenilla.squaremap.common.chunksnapshot.ChunkSnapshotFactory;
import xyz.jpenilla.squaremap.common.chunksnapshot.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.paper.util.Folia;

@DefaultQualifier(NonNull.class)
record PaperChunkSnapshotProvider(
    ServerLevel level,
    Server server,
    JavaPlugin plugin
) implements ChunkSnapshotProvider {
    @Override
    public CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(final int x, final int z) {
        final ChunkTaskScheduler scheduler = this.level.moonrise$getChunkTaskScheduler();
        final @Nullable CompletableFuture<@Nullable CompoundTag> data;
        final ReentrantAreaLock.Node lock = scheduler.schedulingLockArea.lock(x, z);
        try {
            // Keep holder creation/unloading from interleaving with disk-read registration.
            data = scheduler.chunkHolderManager.getChunkHolder(x, z) == null ? this.readChunkData(x, z) : null;
        } finally {
            scheduler.schedulingLockArea.unlock(lock);
        }
        if (data != null) {
            return this.readSnapshot(x, z, data);
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

            return this.chunkSystemSnapshot(x, z);
        }, this.regionExecutor(x, z)).thenCompose(future -> future);
    }

    private CompletableFuture<@Nullable ChunkSnapshot> readSnapshot(int x, int z, final CompletableFuture<@Nullable CompoundTag> data) {
        final ChunkMap chunkMap = this.level.getChunkSource().chunkMap;
        final ChunkTaskScheduler scheduler = this.level.moonrise$getChunkTaskScheduler();
        final Executor executor = task -> scheduler.loadExecutor.createTask(task, Priority.NORMAL).queue();
        final LevelHeightAccessor heightAccessor = LevelHeightAccessor.create(this.level.getMinY(), this.level.getHeight());
        final PalettedContainerFactory palettedContainerFactory = this.level.palettedContainerFactory();
        final DimensionType dimensionType = this.level.dimensionType();
        return data.thenComposeAsync(
            rawTag -> {
                if (rawTag == null) {
                    return CompletableFuture.completedFuture(null);
                }
                final CompoundTag tag = chunkMap.upgradeChunkTag(ChunkSnapshotFactory.ownedForUpgrade(rawTag));
                final ChunkStatus status = tag.read(ChunkDataKeys.STATUS, ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY);
                final @Nullable BelowZeroRetrogen retroGen = tag.read(ChunkDataKeys.RETROGEN, BelowZeroRetrogen.CODEC).orElse(null);
                return switch (ChunkSnapshotEligibility.get(status, retroGen)) {
                    case INELIGIBLE -> CompletableFuture.completedFuture(null);
                    case ELIGIBLE -> {
                        if (ChunkSnapshotFactory.chunkPosMatches(tag, x, z)) {
                            yield CompletableFuture.completedFuture(ChunkSnapshotFactory.snapshotFromChunkData(heightAccessor, dimensionType, palettedContainerFactory, tag));
                        } else {
                            yield this.chunkSystemSnapshot(x, z);
                        }
                    }
                    case NEEDS_RETROGEN -> this.chunkSystemSnapshot(x, z);
                };
            },
            executor
        );
    }

    private CompletableFuture<@Nullable ChunkSnapshot> chunkSystemSnapshot(final int x, final int z) {
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
        }, this.regionExecutor(x, z)).thenCompose(future -> future);
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

    private CompletableFuture<@Nullable CompoundTag> readChunkData(int x, int z) {
        final CompletableFuture<@Nullable CompoundTag> data = new CompletableFuture<>();

        MoonriseRegionFileIO.loadDataAsync(
            this.level,
            x,
            z,
            MoonriseRegionFileIO.RegionFileType.CHUNK_DATA,
            // The data is only read (and copied before any upgrade), so skip Moonrise's defensive copy.
            (BiConsumer<CompoundTag, Throwable> & MoonriseRegionFileIO.NoCopyNBTData) (tag, error) -> {
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

    private static @Nullable ChunkAccess unwrap(@Nullable ChunkAccess chunk) {
        if (chunk == null) {
            return null;
        }
        if (chunk instanceof ImposterProtoChunk imposter) {
            chunk = imposter.getWrapped();
        }
        return chunk;
    }

    private Executor regionExecutor(final int x, final int z) {
        if (!Folia.FOLIA) {
            return task -> {
                if (this.level.getServer().isSameThread()) {
                    task.run();
                    return;
                }
                this.level.getServer().execute(task);
            };
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
