package xyz.jpenilla.squaremap.common.task.render;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.common.util.Numbers;
import xyz.jpenilla.squaremap.common.util.SpiralIterator;
import xyz.jpenilla.squaremap.common.visibilitylimit.VisibilityLimitImpl;

@DefaultQualifier(NonNull.class)
public final class FullRender extends AbstractRender {
    private final DirectoryProvider directoryProvider;
    private final int wait;
    private int maxRadius = 0;
    private int totalChunks;
    private int totalRegions;

    @AssistedInject
    private FullRender(
        @Assisted final MapWorldInternal world,
        @Assisted final int wait,
        final ChunkSnapshotProvider chunkSnapshotProvider,
        final DirectoryProvider directoryProvider
    ) {
        super(world, chunkSnapshotProvider);
        this.directoryProvider = directoryProvider;
        this.wait = wait;
    }

    @AssistedInject
    private FullRender(
        @Assisted final MapWorldInternal world,
        final ChunkSnapshotProvider chunkSnapshotProvider,
        final DirectoryProvider directoryProvider
    ) {
        this(world, 0, chunkSnapshotProvider, directoryProvider);
    }

    @Override
    protected void render() {
        if (this.wait > 0) {
            sleep(this.wait * 1000);
        }

        // order preserved map of regions with boolean to signify if it was already scanned
        final Map<RegionCoordinate, Boolean> regions;

        final @Nullable Map<RegionCoordinate, Boolean> resumedMap = this.mapWorld.getRenderProgress();
        if (resumedMap != null) {
            Logging.info(Lang.LOG_RESUMED_RENDERING, "world", this.mapWorld.identifier().asString());

            regions = resumedMap;

            final int count = (int) regions.values().stream().filter(bool -> bool).count();
            this.processedRegions.set(count);
            this.processedChunks.set(this.countCompletedChunks(regions));
        } else {
            Logging.info(Lang.LOG_STARTED_FULLRENDER, "world", this.mapWorld.identifier().asString());

            // find all region files
            Logging.logger().info(Lang.LOG_SCANNING_REGION_FILES);
            final List<RegionCoordinate> regionFiles = this.getRegions();

            // setup a spiral iterator
            final BlockPos spawn = this.level.getSharedSpawnPos();
            final SpiralIterator<RegionCoordinate> spiral = SpiralIterator.region(
                Numbers.blockToRegion(spawn.getX()),
                Numbers.blockToRegion(spawn.getZ()),
                this.maxRadius
            );

            // iterate the spiral to get all regions needed
            int failsafe = 0;
            regions = new LinkedHashMap<>();
            while (spiral.hasNext() && this.running()) {
                if (failsafe > 500000) {
                    // we scanned over half a million non-existent regions straight
                    // quit the prescan and add the remaining regions to the end
                    regionFiles.forEach(region -> regions.put(region, false));
                    break;
                }
                RegionCoordinate region = spiral.next();
                if (regionFiles.contains(region)) {
                    regions.put(region, false);
                    failsafe = 0;
                } else {
                    failsafe++;
                }
            }
        }

        // ensure task wasnt cancelled before we start
        if (!this.running()) {
            return;
        }

        VisibilityLimitImpl visibility = this.mapWorld.visibilityLimit();
        this.totalRegions = regions.size();
        this.totalChunks = regions.keySet().stream().mapToInt(visibility::countChunksInRegion).sum();

        Logging.info(Lang.LOG_FOUND_TOTAL_REGION_FILES, "total", regions.size());

        this.progress = RenderProgress.printProgress(this, null);

        // finally, scan each region in the order provided by the spiral
        for (final Map.Entry<RegionCoordinate, Boolean> entry : regions.entrySet()) {
            if (!this.running()) {
                break;
            }
            if (entry.getValue()) {
                continue;
            }
            this.mapRegion(entry.getKey());
            entry.setValue(true);
            this.processedRegions.incrementAndGet();
            // only save progress if task is not cancelled
            if (this.running()) {
                this.mapWorld.saveRenderProgress(regions);
            }
        }
    }

    private int countCompletedChunks(final Map<RegionCoordinate, Boolean> regions) {
        final VisibilityLimitImpl visibility = this.mapWorld.visibilityLimit();
        return regions.entrySet().stream()
            .filter(Map.Entry::getValue)
            .mapToInt(entry -> visibility.countChunksInRegion(entry.getKey()))
            .sum();
    }

    @Override
    public int totalChunks() {
        return this.totalChunks;
    }

    @Override
    public int totalRegions() {
        return this.totalRegions;
    }

    private List<RegionCoordinate> getRegions() {
        final List<RegionCoordinate> regions = new ArrayList<>();

        for (final Path path : this.directoryProvider.getRegionFiles(this.level)) {
            if (path.toFile().length() == 0) {
                continue;
            }
            final String[] split = path.getFileName().toString().split("\\.");
            final int x;
            final int z;
            try {
                x = Integer.parseInt(split[1]);
                z = Integer.parseInt(split[2]);
            } catch (final NumberFormatException ex) {
                Logging.logger().warn("Failed to parse coordinates for region file '{}' (file name path: '{}')", path, path.getFileName(), ex);
                continue;
            }

            final RegionCoordinate region = new RegionCoordinate(x, z);

            // ignore regions completely outside the visibility limit
            if (!this.mapWorld.visibilityLimit().shouldRenderRegion(region)) {
                continue;
            }

            this.maxRadius = Math.max(Math.max(this.maxRadius, Math.abs(x)), Math.abs(z));
            regions.add(region);
        }

        return regions;
    }
}
