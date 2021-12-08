package net.pl3x.map.plugin.data;

import java.awt.Color;
import java.awt.image.BufferedImage;
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
import net.pl3x.map.plugin.Logging;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;

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
            final Path dir = Path.of(this.directory.toString(), Integer.toString(this.maxZoom - zoom));
            try {
                Files.createDirectories(dir);
            } catch (final IOException e) {
                Logging.severe(Lang.LOG_COULD_NOT_CREATE_DIR.replace("<path>", dir.toAbsolutePath().toString()), e);
                continue;
            }

            int step = (int) Math.pow(2, zoom);
            int size = SIZE / step;
            int scaledX = Mth.floor((double) this.region.x() / step);
            int scaledZ = Mth.floor((double) this.region.z() / step);

            final String fileName = scaledX + "_" + scaledZ + ".png";
            final Path file = dir.resolve(fileName);

            final BufferedImage image;
            if (Files.isRegularFile(file)) {
                try {
                    image = ImageIO.read(file.toFile());
                } catch (final IOException ex) {
                    try {
                        Files.delete(file);
                    } catch (final IOException x) {
                        ex.addSuppressed(x);
                    }
                    Logging.severe(this.replaceXZ(Lang.LOG_COULD_NOT_READ_REGION), ex);
                    continue;
                }
            } else {
                image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            }

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

            try {
                if (Config.COMPRESS_IMAGES) {
                    final ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
                    try (
                        final OutputStream outputStream = Files.newOutputStream(file);
                        final ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)
                    ) {
                        writer.setOutput(imageOutputStream);
                        ImageWriteParam param = writer.getDefaultWriteParam();
                        if (param.canWriteCompressed()) {
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            if (param.getCompressionType() == null) {
                                param.setCompressionType(param.getCompressionTypes()[0]);
                            }
                            param.setCompressionQuality(Config.COMPRESSION_RATIO);
                        }
                        writer.write(null, new IIOImage(image, null, null), param);
                    }
                } else {
                    ImageIO.write(image, "png", file.toFile());
                }
            } catch (final IOException ex) {
                Logging.severe(this.replaceXZ(Lang.LOG_COULD_NOT_SAVE_REGION), ex);
            }
        }
    }

    private String replaceXZ(final String s) {
        return s.replace("<x>", Integer.toString(this.region.x()))
            .replace("<z>", Integer.toString(this.region.z()));
    }
}
