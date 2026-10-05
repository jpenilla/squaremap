package xyz.jpenilla.squaremap.common.render;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.chunksnapshot.ChunkSnapshotProviderFactory;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.config.RenderConfig;
import xyz.jpenilla.squaremap.common.coordinate.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.coordinate.CoordinateConversions;
import xyz.jpenilla.squaremap.common.coordinate.RegionCoordinate;
import xyz.jpenilla.squaremap.common.coordinate.SpiralIterator;
import xyz.jpenilla.squaremap.common.render.scanning.ScanSettings;
import xyz.jpenilla.squaremap.common.server.ServerAccess;
import xyz.jpenilla.squaremap.common.visibilitylimit.VisibilityLimitImpl;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;

import static xyz.jpenilla.squaremap.common.util.concurrent.Threads.throwIfInterrupted;

@Singleton
@DefaultQualifier(NonNull.class)
public final class RenderFactory {
    private final ChunkSnapshotProviderFactory snapshots;
    private final RegionFileDirectoryResolver directories;
    private final ServerAccess server;

    @Inject
    RenderFactory(final ChunkSnapshotProviderFactory snapshots, final RegionFileDirectoryResolver directories, final ServerAccess server) {
        this.snapshots = snapshots;
        this.directories = directories;
        this.server = server;
    }

    // Each method plans eagerly; background planning claims dirty chunks, so the returned job must be executed.

    RenderJob createFullRender(final MapWorldInternal world, final RenderCheckpointStore checkpoints) throws InterruptedException {
        final ScanSettings settings = ScanSettings.capture(world);
        Logging.info(Messages.LOG_SCANNING_REGION_FILES, "world", world.identifier().asString());
        final RenderPlan plan = RenderPlan.regions(this.findRegions(world, settings.visibility()), settings.visibility()::shouldRenderChunk);
        Logging.info(Messages.LOG_FOUND_TOTAL_REGION_FILES, "total", plan.regions().size(), "world", world.identifier().asString());
        return this.job(world, RenderJob.Mode.FULL, settings, plan, checkpoints);
    }

    RenderJob createRadiusRender(final MapWorldInternal world, final BlockPos center, final int radius, final RenderCheckpointStore checkpoints) throws InterruptedException {
        final ScanSettings settings = ScanSettings.capture(world);
        final RenderPlan plan = RenderPlan.chunks(
            () -> SpiralIterator.chunk(
                CoordinateConversions.blockToChunk(center.getX()), CoordinateConversions.blockToChunk(center.getZ()), CoordinateConversions.blockToChunk(radius)),
            settings.visibility()::shouldRenderChunk
        );
        return this.job(world, RenderJob.Mode.RADIUS, settings, plan, checkpoints);
    }

    RenderJob resumeRender(final MapWorldInternal world, final RenderCheckpointStore.Checkpoint checkpoint, final RenderCheckpointStore checkpoints) {
        final ScanSettings settings = ScanSettings.capture(world);
        // Visibility is evaluated when a job starts; resumed work may shrink but never grows.
        checkpoint.plan().retain(settings.visibility()::shouldRenderChunk);
        return this.job(world, checkpoint.mode(), settings, checkpoint.plan(), checkpoints);
    }

    RenderJob createBackgroundRender(final MapWorldInternal world) throws InterruptedException {
        final ScanSettings settings = ScanSettings.capture(world);
        return this.job(world, RenderJob.Mode.BACKGROUND, settings, planBackground(world, settings.visibility()), null);
    }

    private RenderJob job(
        final MapWorldInternal world,
        final RenderJob.Mode mode,
        final ScanSettings settings,
        final RenderPlan plan,
        final @Nullable RenderCheckpointStore checkpoints
    ) {
        final RenderConfig limits = switch (mode) {
            case FULL -> world.config().FULL_RENDER;
            case RADIUS -> world.config().RADIUS_RENDER;
            case BACKGROUND -> world.config().BACKGROUND_RENDER;
        };
        return new RenderJob(world, this.snapshots, this.server, mode, limits, settings, plan, checkpoints);
    }

    private static RenderPlan planBackground(final MapWorldInternal world, final VisibilityLimitImpl visibility) throws InterruptedException {
        final Set<ChunkCoordinate> selected = new LinkedHashSet<>();
        boolean planned = false;
        try {
            while (world.hasModifiedChunks() && selected.size() < world.config().BACKGROUND_RENDER.maxChunksPerInterval) {
                throwIfInterrupted();
                selected.add(world.nextModifiedChunk());
            }
            final RenderPlan plan = RenderPlan.chunks(selected, visibility::shouldRenderChunk);
            planned = true;
            return plan;
        } finally {
            if (!planned) {
                selected.forEach(world::chunkModified);
            }
        }
    }

    private List<RegionCoordinate> findRegions(final MapWorldInternal world, final VisibilityLimitImpl visibilityLimit) throws InterruptedException {
        final Set<RegionCoordinate> remaining = new LinkedHashSet<>();
        int maxRadius = 0;
        final Path directory = this.directories.resolveRegionFileDirectory(world.serverLevel());
        try (final var paths = Files.list(directory)) {
            for (final Path path : paths.toList()) {
                throwIfInterrupted();
                final String name = path.getFileName().toString();
                if (!name.startsWith("r.") || !(name.endsWith(".mca") || name.endsWith(".linear")) || Files.size(path) == 0) {
                    continue;
                }
                final String[] parts = name.split("\\.");
                try {
                    final RegionCoordinate region = new RegionCoordinate(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    if (visibilityLimit.shouldRenderRegion(region)) {
                        remaining.add(region);
                        maxRadius = Math.max(maxRadius, Math.max(Math.abs(region.x()), Math.abs(region.z())));
                    }
                } catch (final NumberFormatException | IndexOutOfBoundsException ex) {
                    Logging.logger().warn("Failed to parse coordinates for region file '{}'", path, ex);
                }
            }
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to list region files in '" + directory + "'", ex);
        }
        final List<RegionCoordinate> ordered = new ArrayList<>();
        final Iterator<RegionCoordinate> spiral = SpiralIterator.region(0, 0, maxRadius);
        int misses = 0;
        while (spiral.hasNext() && !remaining.isEmpty() && misses <= 500000) {
            throwIfInterrupted();
            final RegionCoordinate region = spiral.next();
            if (remaining.remove(region)) {
                ordered.add(region);
                misses = 0;
            } else {
                misses++;
            }
        }
        ordered.addAll(remaining);
        return ordered;
    }
}
