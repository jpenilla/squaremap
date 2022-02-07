package xyz.jpenilla.squaremap.common.task.render;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.data.Image;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.Numbers;
import xyz.jpenilla.squaremap.common.util.SpiralIterator;
import xyz.jpenilla.squaremap.common.visibilitylimit.VisibilityLimitImpl;

public final class RadiusRender extends AbstractRender {
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final int totalChunks;

    public RadiusRender(final MapWorldInternal world, final @NonNull BlockPos center, int radius) {
        super(world);
        this.radius = Numbers.blockToChunk(radius);
        this.centerX = Numbers.blockToChunk(center.getX());
        this.centerZ = Numbers.blockToChunk(center.getZ());
        this.totalChunks = this.countTotalChunks();
    }

    private int countTotalChunks() {
        int count = 0;
        VisibilityLimitImpl visibility = this.mapWorld.visibilityLimit();
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
        Logging.info(Lang.LOG_STARTED_RADIUSRENDER, "world", this.mapWorld.identifier().asString());

        this.progress = RenderProgress.printProgress(this, null);

        final SpiralIterator<ChunkCoordinate> spiral = SpiralIterator.chunk(this.centerX, this.centerZ, this.radius);
        final Map<RegionCoordinate, Image> images = new HashMap<>();
        final Multimap<RegionCoordinate, CompletableFuture<Void>> futures = ArrayListMultimap.create();

        while (spiral.hasNext()) {
            ChunkCoordinate chunkCoord = spiral.next();
            final RegionCoordinate region = chunkCoord.regionCoordinate();

            // ignore chunks within the radius that are outside the visibility limit
            if (!this.mapWorld.visibilityLimit().shouldRenderChunk(chunkCoord)) {
                continue;
            }

            Image image = images.get(region);
            if (image == null) {
                image = new Image(region, this.mapWorld.tilesPath(), this.mapWorld.config().ZOOM_MAX);
                images.put(region, image);
            }

            futures.put(region, this.mapSingleChunk(image, chunkCoord.x(), chunkCoord.z()));
        }

        final Map<RegionCoordinate, CompletableFuture<Void>> regionFutureMap = new HashMap<>();
        futures.asMap().forEach((region, futureCollection) ->
            regionFutureMap.put(region, CompletableFuture.allOf(futureCollection.toArray(CompletableFuture[]::new))));

        regionFutureMap.forEach((region, combinedFuture) ->
            combinedFuture.whenComplete((result, throwable) -> {
                if (!this.cancelled) {
                    this.mapWorld.saveImage(images.get(region));
                }
            }));

        CompletableFuture.allOf(regionFutureMap.values().toArray(CompletableFuture[]::new)).join();

        if (this.progress != null) {
            this.progress.left().cancel();
        }

    }
}
