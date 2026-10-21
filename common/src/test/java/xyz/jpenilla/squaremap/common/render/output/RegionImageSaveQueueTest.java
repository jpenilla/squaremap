package xyz.jpenilla.squaremap.common.render.output;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.common.coordinate.RegionCoordinate;
import xyz.jpenilla.squaremap.common.coordinate.TileCoordinate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static xyz.jpenilla.squaremap.common.render.output.ZoomDownsamplerTest.image;

@Timeout(10)
class RegionImageSaveQueueTest {
    @TempDir
    Path tiles;

    @Test
    void shutdownWaitsForQueuedWritesDespiteInterruption() throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final RegionImageSaveQueue queue = new RegionImageSaveQueue(this.tiles, 1, 100, blockFirstWorker(entered, release), tile -> { });
        try {
            final RegionImage image = new RegionImage(new RegionCoordinate(0, 0), 0);
            image.setPixel(0, 0, 0xFF00FF00);
            queue.saveImage(image);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertFalse(Files.exists(this.tiles.resolve("0/0_0.png")));
            final CompletableFuture<Boolean> stopped = new CompletableFuture<>();
            Thread.ofPlatform().start(() -> {
                Thread.currentThread().interrupt();
                queue.shutdown();
                stopped.complete(Thread.currentThread().isInterrupted());
            });
            assertThrows(TimeoutException.class, () -> stopped.get(100, TimeUnit.MILLISECONDS));
            release.countDown();
            assertTrue(stopped.get(5, TimeUnit.SECONDS));
            assertEquals(0xFF00FF00, this.read(0, 0, 0).getRGB(0, 0));
        } finally {
            release.countDown();
            queue.shutdown();
        }
    }

    @Test
    void savesAfterShutdownAreRejected() {
        final RegionImageSaveQueue queue = new RegionImageSaveQueue(this.tiles, 1, 1, Executors.defaultThreadFactory(), tile -> { });
        queue.shutdown();
        final RegionImage image = new RegionImage(new RegionCoordinate(0, 0), 0);
        image.setPixel(0, 0, 0xFF00FF00);
        assertThrows(IllegalStateException.class, () -> queue.saveImage(image));
        assertFalse(Files.exists(this.tiles.resolve("0/0_0.png")));
    }

    @Test
    void parallelSavesPreserveSharedFilesAndCorrectionOrder() throws Exception {
        final RegionImageSaveQueue queue = new RegionImageSaveQueue(this.tiles, 2, 100, Executors.defaultThreadFactory(), tile -> { });
        try {
            for (int update = 0; update < 8; update++) {
                final int color = 0xFF000000 | update;
                final RegionImage north = new RegionImage(new RegionCoordinate(0, 0), 1);
                fillBlock(north, 0, 0, color);
                queue.saveImage(north);

                final RegionImage south = new RegionImage(new RegionCoordinate(0, 1), 1);
                fillBlock(south, 0, 512, color);
                queue.saveImage(south);
            }
            final RegionImage correction = new RegionImage(new RegionCoordinate(0, 0), 1);
            fillBlock(correction, 0, 0, 0xFF00FF00);
            queue.saveImage(correction);
        } finally {
            queue.shutdown();
        }

        final BufferedImage shared = this.read(0, 0, 0);
        assertEquals(0xFF00FF00, shared.getRGB(0, 0));
        assertEquals(0xFF000007, shared.getRGB(0, 256));
        assertEquals(0xFF00FF00, this.read(1, 0, 0).getRGB(0, 0));
    }

    @Test
    void coarserLevelsIncludePreviouslyWrittenPixels() throws Exception {
        final RegionImage full = new RegionImage(new RegionCoordinate(0, 0), 1);
        fillBlock(full, 0, 0, 0xFFFF0000);
        this.save(full);

        final RegionImage partial = new RegionImage(new RegionCoordinate(0, 0), 1);
        partial.setPixel(1, 1, 0xFF0000FF);
        this.save(partial);

        final BufferedImage expected = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        ZoomDownsampler.downsample(image(2, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFF0000FF), expected, 0, 0);
        assertEquals(expected.getRGB(0, 0), this.read(0, 0, 0).getRGB(0, 0));
    }

    @Test
    void coarserLevelsPlaceRegionInItsQuadrant() throws Exception {
        final RegionImage image = new RegionImage(new RegionCoordinate(3, -1), 2);
        for (int x = 3 * 512; x < 4 * 512; x++) {
            for (int z = -512; z < 0; z++) {
                image.setPixel(x, z, 0xFF00FF00);
            }
        }
        this.save(image);

        final BufferedImage middle = this.read(1, 1, -1);
        assertEquals(0xFF00FF00, middle.getRGB(256, 256));
        assertEquals(0, middle.getRGB(255, 255));
        final BufferedImage coarsest = this.read(0, 0, -1);
        assertEquals(0xFF00FF00, coarsest.getRGB(384, 384));
        assertEquals(0xFF00FF00, coarsest.getRGB(511, 511));
        assertEquals(0, coarsest.getRGB(383, 383));
    }

    @Test
    void failedLevelSkipsCoarserLevelsButNotOtherFiles() throws Exception {
        // A non-empty directory in place of the north region's tile makes its write fail
        Files.createDirectories(this.tiles.resolve("1/0_0.png/blocker"));
        final RegionImage north = new RegionImage(new RegionCoordinate(0, 0), 1);
        fillBlock(north, 0, 0, 0xFFFF0000);
        final RegionImage south = new RegionImage(new RegionCoordinate(0, 1), 1);
        fillBlock(south, 0, 512, 0xFF0000FF);
        this.save(north, south);

        final BufferedImage shared = this.read(0, 0, 0);
        assertEquals(0, shared.getRGB(0, 0));
        assertEquals(0xFF0000FF, shared.getRGB(0, 256));
    }

    @Test
    void reportsEachTileAfterItsFileIsWrittenFinestFirst() throws Exception {
        final List<TileCoordinate> written = new CopyOnWriteArrayList<>();
        final RegionImageSaveQueue queue = new RegionImageSaveQueue(this.tiles, 2, 100, Executors.defaultThreadFactory(), tile -> {
            if (Files.isRegularFile(this.tiles.resolve(tile.zoom() + "/" + tile.x() + "_" + tile.z() + ".png"))) {
                written.add(tile);
            }
        });
        try {
            final RegionImage image = new RegionImage(new RegionCoordinate(0, 0), 1);
            image.setPixel(0, 0, 0xFFFF0000);
            queue.saveImage(image);
        } finally {
            queue.shutdown();
        }
        assertEquals(List.of(new TileCoordinate(1, 0, 0), new TileCoordinate(0, 0, 0)), written);
    }

    private void save(final RegionImage... images) throws InterruptedException {
        final RegionImageSaveQueue queue = new RegionImageSaveQueue(this.tiles, 2, 100, Executors.defaultThreadFactory(), tile -> { });
        try {
            for (final RegionImage image : images) {
                queue.saveImage(image);
            }
        } finally {
            queue.shutdown();
        }
    }

    private BufferedImage read(final int zoom, final int x, final int z) throws Exception {
        return ImageIO.read(this.tiles.resolve(zoom + "/" + x + "_" + z + ".png").toFile());
    }

    // Fills the 2×2 block that becomes one pixel at the next zoom level
    private static void fillBlock(final RegionImage image, final int x, final int z, final int color) {
        image.setPixel(x, z, color);
        image.setPixel(x + 1, z, color);
        image.setPixel(x, z + 1, color);
        image.setPixel(x + 1, z + 1, color);
    }

    private static ThreadFactory blockFirstWorker(final CountDownLatch entered, final CountDownLatch release) {
        final AtomicInteger threads = new AtomicInteger();
        return task -> {
            final boolean first = threads.getAndIncrement() == 0;
            return new Thread(() -> {
                if (first) {
                    entered.countDown();
                    try {
                        release.await();
                    } catch (final InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
                task.run();
            });
        };
    }
}
