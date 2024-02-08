package xyz.jpenilla.squaremap.common.task.render;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.RegionFileDirectoryResolver;
import xyz.jpenilla.squaremap.common.util.SpiralIterator;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshotProviderFactory;
import xyz.jpenilla.squaremap.common.visibilitylimit.VisibilityLimitImpl;

@DefaultQualifier(NonNull.class)
public final class FullRender extends AbstractRender {
    private final RegionFileDirectoryResolver regionFileDirectoryResolver;
    private final int wait;
    private int maxRadius = 0;
    private int totalChunks;
    private int totalRegions;

    @AssistedInject
    private FullRender(
        @Assisted final MapWorldInternal world,
        @Assisted final int wait,
        final ChunkSnapshotProviderFactory chunkSnapshotProviderFactory,
        final RegionFileDirectoryResolver regionFileDirectoryResolver
    ) {
        super(world, chunkSnapshotProviderFactory);
        this.regionFileDirectoryResolver = regionFileDirectoryResolver;
        this.wait = wait;
    }

    @AssistedInject
    private FullRender(
        @Assisted final MapWorldInternal world,
        final ChunkSnapshotProviderFactory chunkSnapshotProviderFactory,
        final RegionFileDirectoryResolver regionFileDirectoryResolver
    ) {
        this(world, 0, chunkSnapshotProviderFactory, regionFileDirectoryResolver);
    }

    @Override
    protected void render() {
        if (this.wait > 0) {
            sleep(this.wait * 1000);
        }

        // order preserved map of regions with boolean to signify if it was already scanned
        final Map<RegionCoordinate, Boolean> regions;

        final @Nullable Map<RegionCoordinate, Boolean> resumedMap = this.mapWorld.renderManager().readRenderProgress();
        if (resumedMap != null) {
            Logging.info(Messages.LOG_RESUMED_RENDERING, "world", this.mapWorld.identifier().asString());

            regions = resumedMap;

            final int count = (int) regions.values().stream().filter(bool -> bool).count();
            this.processedRegions.set(count);
            this.processedChunks.set(this.countCompletedChunks(regions));
        } else {
            Logging.info(Messages.LOG_STARTED_FULLRENDER, "world", this.mapWorld.identifier().asString());

            // find all region files
            Logging.logger().info(Messages.LOG_SCANNING_REGION_FILES);
            final List<RegionCoordinate> regionFiles = this.getRegions();

            // setup a spiral iterator
            final Iterator<RegionCoordinate> spiral = SpiralIterator.region(0, 0, this.maxRadius);

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
                final RegionCoordinate region = spiral.next();
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

        final VisibilityLimitImpl visibility = this.mapWorld.visibilityLimit();
        this.totalRegions = regions.size();
        this.totalChunks = regions.keySet().stream().mapToInt(visibility::countChunksInRegion).sum();

        Logging.info(Messages.LOG_FOUND_TOTAL_REGION_FILES, "total", regions.size());

        this.progress = RenderProgress.printProgress(this);

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
                this.mapWorld.renderManager().saveRenderProgress(regions);
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
        final Set<RegionCoordinate> regions = new LinkedHashSet<>();

        for (final RegionCoordinate region : this.getRegionsRaw()) {
            // ignore regions completely outside the visibility limit
            if (!this.mapWorld.visibilityLimit().shouldRenderRegion(region)) {
                continue;
            }

            this.maxRadius = Math.max(Math.max(this.maxRadius, Math.abs(region.x())), Math.abs(region.z()));
            regions.add(region);
        }

        return List.copyOf(regions);
    }

    private List<RegionCoordinate> getRegionsRaw() {
        final Path regionFolder = this.regionFileDirectoryResolver.resolveRegionFileDirectory(this.level);
        Logging.debug(() -> "Listing region files for directory '" + regionFolder + "'...");
        try (final Stream<Path> stream = Files.list(regionFolder)) {
            return stream.map(file -> {
                final String fileName = file.getFileName().toString();
                if (!fileName.startsWith("r.")) {
                    return parse(file, 0);
                } else if (fileName.endsWith(".mca") || fileName.endsWith(".linear")) {
                    return parse(file, 1);
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to list region files in directory '" + regionFolder.toAbsolutePath() + "'", ex);
        }
    }

    private static @Nullable RegionCoordinate parse(final Path path, final int offset) {
        if (path.toFile().length() == 0) {
            return null;
        }
        final String[] split = path.getFileName().toString().split("\\.");
        try {
            final int x = Integer.parseInt(split[0] + offset);
            final int z = Integer.parseInt(split[1] + offset);
            return new RegionCoordinate(x, z);
        } catch (final NumberFormatException ex) {
            Logging.logger().warn("Failed to parse coordinates for region file '{}' (file name path: '{}')", path, path.getFileName(), ex);
            return null;
        }
    }
}
