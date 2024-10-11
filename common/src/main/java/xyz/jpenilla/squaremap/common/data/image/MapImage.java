package xyz.jpenilla.squaremap.common.data.image;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.minecraft.util.Mth;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.FileUtil;

@DefaultQualifier(NonNull.class)
public final class MapImage {
    private static final int TRANSPARENT = new Color(0, 0, 0, 0).getRGB();
    public static final int SIZE = 512;
    private final MapImageIO<MapImageIO.IOMapImage> backend;
    private final RegionCoordinate region;
    private final Path directory;
    private final int maxZoom;
    private int @Nullable [][] pixels = null;

    @SuppressWarnings("unchecked")
    public MapImage(
        final RegionCoordinate region,
        final Path directory,
        final int maxZoom,
        final MapImageIO<?> backend
    ) {
        this.region = region;
        this.directory = directory;
        this.maxZoom = maxZoom;
        this.backend = (MapImageIO<MapImageIO.IOMapImage>) backend;
    }

    public synchronized void setPixel(final int x, final int z, final int color) {
        if (this.pixels == null) {
            this.pixels = new int[SIZE][SIZE];
            for (final int[] arr : this.pixels) {
                Arrays.fill(arr, Integer.MIN_VALUE);
            }
        }

        this.pixels[x & (SIZE - 1)][z & (SIZE - 1)] = color;
    }

    public synchronized void save() {
        if (this.pixels == null) {
            return;
        }

        for (int zoom = 0; zoom <= this.maxZoom; zoom++) {
            int step = (int) Math.pow(2, zoom);
            int size = SIZE / step;
            int scaledX = Mth.floor((double) this.region.x() / step);
            int scaledZ = Mth.floor((double) this.region.z() / step);

            final MapImageIO.IOMapImage image = this.getOrCreate(this.maxZoom - zoom, scaledX, scaledZ);

            int baseX = (this.region.x() * size) & (SIZE - 1);
            int baseZ = (this.region.z() * size) & (SIZE - 1);
            for (int x = 0; x < SIZE; x += step) {
                for (int z = 0; z < SIZE; z += step) {
                    final int pixel = this.pixels[x][z];
                    if (pixel != Integer.MIN_VALUE) {
                        final int color = pixel == 0 ? TRANSPARENT : pixel;
                        image.setPixel(baseX + (x / step), baseZ + (z / step), color);
                    }
                }
            }

            this.saveImage(this.maxZoom - zoom, scaledX, scaledZ, image);
        }
    }

    private MapImageIO.IOMapImage getOrCreate(final int zoom, final int scaledX, final int scaledZ) {
        final Path file = this.imageInDirectory(zoom, scaledX, scaledZ);

        if (!Files.isRegularFile(file)) {
            return this.backend.newImage();
        }

        try {
            return this.backend.load(file);
        } catch (final IOException ex) {
            try {
                Files.deleteIfExists(file);
            } catch (final IOException ex0) {
                ex.addSuppressed(ex0);
            }
            this.logCouldNotRead(ex);
            return this.backend.newImage();
        }
    }

    private void saveImage(final int zoom, final int scaledX, final int scaledZ, final MapImageIO.IOMapImage image) {
        final Path out = this.imageInDirectory(zoom, scaledX, scaledZ);
        try {
            FileUtil.atomicWrite(out, tmp -> {
                try (final OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(tmp))) {
                    this.backend.save(image, outputStream);
                }
            });
        } catch (final IOException ex) {
            this.logCouldNotSave(ex);
        }
    }

    private Path imageInDirectory(final int zoom, final int scaledX, final int scaledZ) {
        final Path dir = this.directory.resolve(Integer.toString(zoom));
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (final IOException e) {
                throw new RuntimeException(Logging.replace(Messages.LOG_COULD_NOT_CREATE_DIR, "path", dir.toAbsolutePath()), e);
            }
        }
        final String fileName = scaledX + "_" + scaledZ + ".png";
        return dir.resolve(fileName);
    }

    private void logCouldNotRead(final IOException ex) {
        Logging.logger().error(xz(Messages.LOG_COULD_NOT_READ_REGION), ex);
    }

    private void logCouldNotSave(final IOException ex) {
        Logging.logger().error(xz(Messages.LOG_COULD_NOT_SAVE_REGION), ex);
    }

    private String xz(final String s) {
        return Logging.replace(s, "x", this.region.x(), "z", this.region.z());
    }
}
