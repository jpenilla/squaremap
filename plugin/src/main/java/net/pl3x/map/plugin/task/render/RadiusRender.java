package net.pl3x.map.plugin.task.render;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.WorldManager;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.data.ChunkCoordinate;
import net.pl3x.map.plugin.data.Image;
import net.pl3x.map.plugin.data.Region;
import net.pl3x.map.plugin.util.Numbers;
import net.pl3x.map.plugin.util.iterator.ChunkSpiralIterator;
import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;

public final class RadiusRender extends AbstractRender {
    final int centerX;
    final int centerZ;
    final int radius;
    final int totalChunks;

    public RadiusRender(final @NonNull Location center, int radius) {
        super(WorldManager.getWorld(center.getWorld()));
        this.radius = Numbers.blockToChunk(radius);
        this.centerX = Numbers.blockToChunk(center.getBlockX());
        this.centerZ = Numbers.blockToChunk(center.getBlockZ());
        this.totalChunks = (int) Math.pow((this.radius * 2) + 1, 2);
    }

    @Override
    public int totalChunks() {
        return this.totalChunks;
    }

    @Override
    protected void render() {
        Logger.info(Lang.LOG_STARTED_RADIUSRENDER
                .replace("{world}", world.getName()));

        final Timer timer = RenderProgress.printProgress(this);

        ChunkSpiralIterator spiral = new ChunkSpiralIterator(this.centerX, this.centerZ, this.radius);
        final Map<Region, Image> images = new HashMap<>();
        final Multimap<Region, CompletableFuture<Void>> futures = ArrayListMultimap.create();

        while (spiral.hasNext()) {
            ChunkCoordinate chunkCoord = spiral.next();
            final Region region = chunkCoord.regionCoordinate();

            Image image = images.get(region);
            if (image == null) {
                image = new Image(region, worldTilesDir, worldConfig.ZOOM_MAX);
                images.put(region, image);
            }

            futures.put(region, this.mapSingleChunk(image, chunkCoord.getX(), chunkCoord.getZ()));
        }

        final Map<Region, CompletableFuture<Void>> regionFutureMap = new HashMap<>();
        futures.asMap().forEach((region, futureCollection) ->
                regionFutureMap.put(region, CompletableFuture.allOf(futureCollection.toArray(CompletableFuture[]::new))));

        regionFutureMap.forEach((region, combinedFuture) ->
                combinedFuture.whenComplete((result, throwable) -> {
                    if (!this.cancelled) {
                        this.mapWorld.saveImage(images.get(region));
                    }
                }));

        CompletableFuture.allOf(regionFutureMap.values().toArray(CompletableFuture[]::new)).join();

        timer.cancel();

        Logger.info(Lang.LOG_FINISHED_RENDERING
                .replace("{world}", world.getName()));

    }
}
