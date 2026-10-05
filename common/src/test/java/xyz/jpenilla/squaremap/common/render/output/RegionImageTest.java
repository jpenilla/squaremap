package xyz.jpenilla.squaremap.common.render.output;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.common.coordinate.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.CheckedRunnable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RegionImageTest {
    @TempDir
    Path tiles;

    @Test
    void writesPreserveUntouchedPixelsAndShareZoomTiles() throws Exception {
        final RegionImage north = new RegionImage(new RegionCoordinate(0, 0), this.tiles, 1);
        north.setPixel(0, 0, 0xFFFF0000);
        north.setPixel(0, 1, 0xFFFF0000);
        save(north);

        final RegionImage south = new RegionImage(new RegionCoordinate(0, 1), this.tiles, 1);
        south.setPixel(0, 512, 0xFF0000FF);
        save(south);

        final var shared = ImageIO.read(this.tiles.resolve("0/0_0.png").toFile());
        assertEquals(0xFFFF0000, shared.getRGB(0, 0));
        assertEquals(0xFF0000FF, shared.getRGB(0, 256));

        final RegionImage correction = new RegionImage(new RegionCoordinate(0, 0), this.tiles, 1);
        correction.setPixel(0, 0, 0xFF0000FF);
        save(correction);
        final var detailed = ImageIO.read(this.tiles.resolve("1/0_0.png").toFile());
        assertEquals(0xFF0000FF, detailed.getRGB(0, 0));
        assertEquals(0xFFFF0000, detailed.getRGB(0, 1));
    }

    @Test
    void absentOutputDoesNotCreateTiles() throws Exception {
        final RegionImage image = new RegionImage(new RegionCoordinate(0, 0), this.tiles, 1);
        save(image);
        assertFalse(Files.exists(this.tiles.resolve("1/0_0.png")));
    }

    private static void save(final RegionImage image) throws IOException {
        for (final CheckedRunnable<IOException> task : image.saveTasks().values()) {
            task.run();
        }
    }
}
