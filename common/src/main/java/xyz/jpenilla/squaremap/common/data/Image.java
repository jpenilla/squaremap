package xyz.jpenilla.squaremap.common.data;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import net.minecraft.util.Mth;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.util.FileUtil;

@DefaultQualifier(NonNull.class)
public final class Image {
    private static final int TRANSPARENT = new Color(0, 0, 0, 0).getRGB();
    public static final int SIZE = 512;
    private final int[][] pixels = new int[SIZE][SIZE];
    private final int maxZoom;
    private final RegionCoordinate region;
    private final Path directory;

    public Image(final RegionCoordinate region, final Path directory, final int maxZoom) {
        this.region = region;
        this.directory = directory;
        this.maxZoom = maxZoom;
        for (int[] arr : this.pixels) {
            Arrays.fill(arr, Integer.MIN_VALUE);
        }
    }

    public synchronized void setPixel(final int x, final int z, final int color) {
        this.pixels[x & (SIZE - 1)][z & (SIZE - 1)] = color;
    }

    public void save() {
        for (int zoom = 0; zoom <= this.maxZoom; zoom++) {
            int step = (int) Math.pow(2, zoom);
            int size = SIZE / step;
            int scaledX = Mth.floor((double) this.region.x() / step);
            int scaledZ = Mth.floor((double) this.region.z() / step);

            final BufferedImage image = this.getOrCreate(this.maxZoom - zoom, scaledX, scaledZ);

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

            this.save(this.maxZoom - zoom, scaledX, scaledZ, image);
        }
    }

    private BufferedImage getOrCreate(final int zoom, final int scaledX, final int scaledZ) {
        final Path file = this.imageInDirectory(zoom, scaledX, scaledZ);

        if (!Files.isRegularFile(file)) {
            return newBufferedImage();
        }

        try {
            return ImageIO.read(file.toFile());
        } catch (final IOException ex) {
            try {
                Files.deleteIfExists(file);
            } catch (final IOException ex0) {
                ex.addSuppressed(ex0);
            }
            this.logCouldNotRead(ex);
            return newBufferedImage();
        }
    }

    private void save(final int zoom, final int scaledX, final int scaledZ, final BufferedImage image) {
        final Path out = this.imageInDirectory(zoom, scaledX, scaledZ);
        final Path tmp = FileUtil.siblingTempFile(out);
        try (final OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(tmp))) {
            save(image, outputStream);
            FileUtil.atomicMove(tmp, out, true);
        } catch (final IOException ex) {
            try {
                Files.deleteIfExists(tmp);
            } catch (final IOException ex0) {
                ex.addSuppressed(ex0);
            }
            this.logCouldNotSave(ex);
        }
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

    private Path imageInDirectory(final int zoom, final int scaledX, final int scaledZ) {
        final Path dir = this.directory.resolve(Integer.toString(zoom));
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (final IOException e) {
                throw new RuntimeException(Logging.replace(Lang.LOG_COULD_NOT_CREATE_DIR, "path", dir.toAbsolutePath()), e);
            }
        }
        final String fileName = scaledX + "_" + scaledZ + ".png";
        return dir.resolve(fileName);
    }

    private static BufferedImage newBufferedImage() {
        return new BufferedImage(Image.SIZE, Image.SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private void logCouldNotRead(final IOException ex) {
        Logging.logger().error(xz(Lang.LOG_COULD_NOT_READ_REGION), ex);
    }

    private void logCouldNotSave(final IOException ex) {
        Logging.logger().error(xz(Lang.LOG_COULD_NOT_SAVE_REGION), ex);
    }

    private String xz(final String s) {
        return Logging.replace(s, "x", this.region.x(), "z", this.region.z());
    }
}
