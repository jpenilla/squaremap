package net.pl3x.map.task;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;
import net.pl3x.map.Logger;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.data.Image;
import net.pl3x.map.data.Region;
import net.pl3x.map.util.Numbers;
import net.pl3x.map.util.iterator.ChunkSpiralIterator;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RadiusRender extends AbstractRender {
    final int centerX;
    final int centerZ;
    final int radius;
    final int totalChunks;

    public RadiusRender(Location center, int radius) {
        super(center.getWorld());
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

        ChunkSpiralIterator spiral = new ChunkSpiralIterator(this.centerX, this.centerZ, this.radius);
        final Map<Region, Image> images = new HashMap<>();
        final Multimap<Region, CompletableFuture<Void>> futures = ArrayListMultimap.create();

        while (spiral.hasNext()) {
            ChunkCoordIntPair pair = spiral.next();
            final Region region = new Region(pair.getRegionX(), pair.getRegionZ());

            Image image = images.get(region);
            if (image == null) {
                image = new Image(region, worldTilesDir, worldConfig.ZOOM_MAX);
                images.put(region, image);
            }

            futures.put(region, this.mapSingleChunk(image, pair.x, pair.z));
        }

        final Map<Region, CompletableFuture<Void>> regionFutureMap = new HashMap<>();
        futures.asMap().forEach((region, futureCollection) ->
                regionFutureMap.put(region, CompletableFuture.allOf(futureCollection.toArray(CompletableFuture[]::new))));

        final ExecutorService imageSave = Executors.newSingleThreadExecutor();

        regionFutureMap.forEach((region, combinedFuture) ->
                combinedFuture.whenComplete((completedFuture, throwable) ->
                        imageSave.submit(() -> {
                            if (!this.cancelled) {
                                images.get(region).save();
                            }
                        })));

        CompletableFuture.allOf(regionFutureMap.values().toArray(CompletableFuture[]::new)).join();

        imageSave.shutdown();
    }
}
