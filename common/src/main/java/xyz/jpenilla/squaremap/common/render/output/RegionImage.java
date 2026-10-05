package xyz.jpenilla.squaremap.common.render.output;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.coordinate.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.CheckedRunnable;
import xyz.jpenilla.squaremap.common.util.FileUtil;

@DefaultQualifier(NonNull.class)
public final class RegionImage {
    private static final int TRANSPARENT = new Color(0, 0, 0, 0).getRGB();
    private static final int SIZE = 512;
    private final RegionCoordinate region;
    private final Path directory;
    private final int maxZoom;
    private int @Nullable [][] pixels = null;

    public RegionImage(final RegionCoordinate region, final Path directory, final int maxZoom) {
        this.region = region;
        this.directory = directory;
        this.maxZoom = maxZoom;
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

    /**
     * Creates independent file updates, listed from finest to coarsest zoom.
     * Pixels must be finished before submission and remain unchanged until all tasks complete.
     * The caller must serialize tasks targeting the same destination file.
     */
    Map<Path, CheckedRunnable<IOException>> saveTasks() {
        final Map<Path, CheckedRunnable<IOException>> tasks = new LinkedHashMap<>();
        for (int zoom = 0; zoom <= this.maxZoom; zoom++) {
            final int step = 1 << zoom;
            final Path file = this.directory.resolve(Integer.toString(this.maxZoom - zoom))
                .resolve(Math.floorDiv(this.region.x(), step) + "_" + Math.floorDiv(this.region.z(), step) + ".png")
                .toAbsolutePath().normalize();
            tasks.put(file, () -> {
                if (this.pixels != null) {
                    this.saveZoom(file, step);
                }
            });
        }
        return tasks;
    }

    private void saveZoom(final Path file, final int step) throws IOException {
        final int size = SIZE / step;
        final BufferedImage image = this.getOrCreate(file);

        int baseX = (this.region.x() * size) & (SIZE - 1);
        int baseZ = (this.region.z() * size) & (SIZE - 1);
        for (int x = 0; x < SIZE; x += step) {
            for (int z = 0; z < SIZE; z += step) {
                final int pixel = this.pixels[x][z];
                if (pixel != Integer.MIN_VALUE) {
                    final int color = pixel == 0 ? TRANSPARENT : pixel;
                    image.setRGB(baseX + (x / step), baseZ + (z / step), color);
                }
            }
        }

        save(file, image);
    }

    private BufferedImage getOrCreate(final Path file) throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.isRegularFile(file)) {
            return newBufferedImage();
        }

        try {
            final @Nullable BufferedImage read = ImageIO.read(file.toFile());
            if (read == null) {
                throw new IOException("No supported image reader could read the file");
            }
            return read;
        } catch (final IOException ex) {
            try {
                Files.deleteIfExists(file);
            } catch (final IOException ex0) {
                ex.addSuppressed(ex0);
            }
            Logging.error(Messages.LOG_COULD_NOT_READ_IMAGE, ex, "path", file);
            return newBufferedImage();
        }
    }

    private static void save(final Path out, final BufferedImage image) throws IOException {
        FileUtil.atomicWrite(out, tmp -> {
            try (final OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(tmp))) {
                save(image, outputStream);
            }
        });
    }

    private static void save(final BufferedImage image, final OutputStream out) throws IOException {
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        try (final ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(imageOutputStream);
            final ImageWriteParam param = writer.getDefaultWriteParam();
            if (Config.COMPRESS_IMAGES && param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                if (param.getCompressionType() == null) {
                    param.setCompressionType(param.getCompressionTypes()[0]);
                }
                param.setCompressionQuality(Config.COMPRESSION_RATIO);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        }
    }

    private static BufferedImage newBufferedImage() {
        return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }
}
