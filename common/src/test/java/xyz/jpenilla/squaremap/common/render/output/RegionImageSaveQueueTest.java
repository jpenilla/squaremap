package xyz.jpenilla.squaremap.common.render.output;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(10)
class RegionImageSaveQueueTest {
    @TempDir
    Path tiles;

    @Test
    void drainWaitsForQueuedWritesDespiteInterruption() throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final RegionImageSaveQueue queue = new RegionImageSaveQueue(1, 100, blockFirstWorker(entered, release));
        try {
            final RegionImage image = new RegionImage(new RegionCoordinate(0, 0), this.tiles, 0);
            image.setPixel(0, 0, 0xFF00FF00);
            queue.saveImage(image);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertFalse(Files.exists(this.tiles.resolve("0/0_0.png")));
            final CompletableFuture<Boolean> drained = new CompletableFuture<>();
            Thread.ofPlatform().start(() -> {
                Thread.currentThread().interrupt();
                queue.drain();
                drained.complete(Thread.currentThread().isInterrupted());
            });
            assertThrows(TimeoutException.class, () -> drained.get(100, TimeUnit.MILLISECONDS));
            release.countDown();
            assertTrue(drained.get(5, TimeUnit.SECONDS));
            assertEquals(0xFF00FF00, ImageIO.read(this.tiles.resolve("0/0_0.png").toFile()).getRGB(0, 0));
        } finally {
            release.countDown();
            queue.shutdown();
        }
    }

    @Test
    void shutdownWaitsForQueuedWrites() throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final RegionImageSaveQueue queue = new RegionImageSaveQueue(1, 100, blockFirstWorker(entered, release));
        try {
            final RegionImage image = new RegionImage(new RegionCoordinate(0, 0), this.tiles, 0);
            image.setPixel(0, 0, 0xFFFF0000);
            queue.saveImage(image);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            final CompletableFuture<Void> stopped = CompletableFuture.runAsync(queue::shutdown);
            assertThrows(TimeoutException.class, () -> stopped.get(100, TimeUnit.MILLISECONDS));
            release.countDown();
            stopped.get(5, TimeUnit.SECONDS);
            assertEquals(0xFFFF0000, ImageIO.read(this.tiles.resolve("0/0_0.png").toFile()).getRGB(0, 0));
        } finally {
            release.countDown();
            queue.shutdown();
        }
    }

    @Test
    void parallelSavesPreserveSharedFilesAndCorrectionOrder() throws Exception {
        final RegionImageSaveQueue queue = new RegionImageSaveQueue(2, 100, Executors.defaultThreadFactory());
        try {
            for (int update = 0; update < 8; update++) {
                final int color = 0xFF000000 | update;
                final RegionImage north = new RegionImage(new RegionCoordinate(0, 0), this.tiles, 1);
                north.setPixel(0, 0, color);
                queue.saveImage(north);

                final RegionImage south = new RegionImage(new RegionCoordinate(0, 1), this.tiles, 1);
                south.setPixel(0, 512, color);
                queue.saveImage(south);
            }
            final RegionImage correction = new RegionImage(new RegionCoordinate(0, 0), this.tiles, 1);
            correction.setPixel(0, 0, 0xFF00FF00);
            queue.saveImage(correction);
            queue.drain();

            final var shared = ImageIO.read(this.tiles.resolve("0/0_0.png").toFile());
            assertEquals(0xFF00FF00, shared.getRGB(0, 0));
            assertEquals(0xFF000007, shared.getRGB(0, 256));
            final var detailed = ImageIO.read(this.tiles.resolve("1/0_0.png").toFile());
            assertEquals(0xFF00FF00, detailed.getRGB(0, 0));
        } finally {
            queue.shutdown();
        }
    }

    @Test
    void failedFileDoesNotPreventOtherFilesOrLaterSaves() throws Exception {
        Files.writeString(this.tiles.resolve("1"), "not a directory");
        final RegionImageSaveQueue queue = new RegionImageSaveQueue(2, 100, Executors.defaultThreadFactory());
        try {
            for (final int color : new int[] {0xFFFF0000, 0xFF0000FF}) {
                final RegionImage image = new RegionImage(new RegionCoordinate(0, 0), this.tiles, 1);
                image.setPixel(0, 0, color);
                queue.saveImage(image);
            }
            queue.drain();
            assertEquals(0xFF0000FF, ImageIO.read(this.tiles.resolve("0/0_0.png").toFile()).getRGB(0, 0));
        } finally {
            queue.shutdown();
        }
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
