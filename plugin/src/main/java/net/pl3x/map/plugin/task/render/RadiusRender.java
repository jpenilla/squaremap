package net.pl3x.map.plugin.task.render;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Logging;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.data.ChunkCoordinate;
import net.pl3x.map.plugin.data.Image;
import net.pl3x.map.plugin.data.RegionCoordinate;
import net.pl3x.map.plugin.util.Numbers;
import net.pl3x.map.plugin.util.iterator.ChunkSpiralIterator;
import net.pl3x.map.plugin.visibilitylimit.VisibilityLimit;
import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class RadiusRender extends AbstractRender {
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final int totalChunks;

    public RadiusRender(final @NonNull Location center, int radius) {
        super(Pl3xMapPlugin.getInstance().worldManager().getWorld(center.getWorld()));
        this.radius = Numbers.blockToChunk(radius);
        this.centerX = Numbers.blockToChunk(center.getBlockX());
        this.centerZ = Numbers.blockToChunk(center.getBlockZ());
        this.totalChunks = this.countTotalChunks();
    }

    private int countTotalChunks() {
        int count = 0;
        VisibilityLimit visibility = this.mapWorld.visibilityLimit();
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
        Logging.info(Lang.LOG_STARTED_RADIUSRENDER, Template.template("world", world.getName()));

        this.progress = RenderProgress.printProgress(this, null);

        ChunkSpiralIterator spiral = new ChunkSpiralIterator(this.centerX, this.centerZ, this.radius);
        final Map<RegionCoordinate, Image> images = new HashMap<>();
        final Multimap<RegionCoordinate, CompletableFuture<Void>> futures = ArrayListMultimap.create();

        while (spiral.hasNext()) {
            ChunkCoordinate chunkCoord = spiral.next();
            final RegionCoordinate region = chunkCoord.regionCoordinate();

            // ignore chunks within the radius that are outside the visibility limit
            if (!mapWorld.visibilityLimit().shouldRenderChunk(chunkCoord)) {
                continue;
            }

            Image image = images.get(region);
            if (image == null) {
                image = new Image(region, worldTilesDir, mapWorld.config().ZOOM_MAX);
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
