package xyz.jpenilla.squaremap.common.task.render;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.data.Image;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.common.util.Numbers;
import xyz.jpenilla.squaremap.common.util.SpiralIterator;
import xyz.jpenilla.squaremap.common.visibilitylimit.VisibilityLimitImpl;

@DefaultQualifier(NonNull.class)
public final class RadiusRender extends AbstractRender {
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final int totalChunks;

    @AssistedInject
    private RadiusRender(
        @Assisted final MapWorldInternal world,
        @Assisted final BlockPos center,
        @Assisted final int radius,
        final ChunkSnapshotProvider chunkSnapshotProvider
    ) {
        super(world, chunkSnapshotProvider);
        this.radius = Numbers.blockToChunk(radius);
        this.centerX = Numbers.blockToChunk(center.getX());
        this.centerZ = Numbers.blockToChunk(center.getZ());
        this.totalChunks = this.countTotalChunks();
    }

    private int countTotalChunks() {
        int count = 0;
        final VisibilityLimitImpl visibility = this.mapWorld.visibilityLimit();
        for (int chunkX = this.centerX - this.radius; chunkX <= this.centerX + this.radius; chunkX++) {
            for (int chunkZ = this.centerZ - this.radius; chunkZ <= this.centerZ + this.radius; chunkZ++) {
                if (visibility.shouldRenderChunk(chunkX, chunkZ)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public int totalChunks() {
        return this.totalChunks;
    }

    @Override
    public int totalRegions() {
        return -1; // we only count chunks for radius render
    }

    @Override
    protected void render() {
        Logging.info(Messages.LOG_STARTED_RADIUSRENDER, "world", this.mapWorld.identifier().asString());

        this.progress = RenderProgress.printProgress(this);

        final Iterator<ChunkCoordinate> spiral = SpiralIterator.chunk(this.centerX, this.centerZ, this.radius);
        final Map<RegionCoordinate, List<ChunkCoordinate>> chunks = new LinkedHashMap<>();

        while (spiral.hasNext() && this.running()) {
            final ChunkCoordinate chunkCoord = spiral.next();
            final RegionCoordinate region = chunkCoord.regionCoordinate();

            // ignore chunks within the radius that are outside the visibility limit
            if (!this.mapWorld.visibilityLimit().shouldRenderChunk(chunkCoord)) {
                continue;
            }

            chunks.computeIfAbsent(region, $ -> new ArrayList<>()).add(chunkCoord);
        }

        for (final Map.Entry<RegionCoordinate, List<ChunkCoordinate>> entry : chunks.entrySet()) {
            if (!this.running()) {
                break;
            }
            final RegionCoordinate region = entry.getKey();
            final List<ChunkCoordinate> chunkCoords = entry.getValue();
            if (chunkCoords.size() == 1024) {
                this.mapRegion(region);
                continue;
            }
            final Image image = new Image(region, this.mapWorld.tilesPath(), this.mapWorld.config().ZOOM_MAX);
            final List<CompletableFuture<Void>> chunkFutures = new ArrayList<>();
            for (final ChunkCoordinate chunkCoord : chunkCoords) {
                chunkFutures.add(this.mapSingleChunk(image, chunkCoord.x(), chunkCoord.z()));
            }
            try {
                CompletableFuture.allOf(chunkFutures.toArray(CompletableFuture[]::new)).get();
            } catch (final InterruptedException ignore) {
                break;
            } catch (final CancellationException | ExecutionException ex) {
                Logging.logger().error("Exception executing radius render for region {}", region, ex);
                break;
            }
            if (this.running()) {
                this.mapWorld.saveImage(image);
            }
        }
    }
}
