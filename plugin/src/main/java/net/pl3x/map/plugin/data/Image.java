package net.pl3x.map.plugin.data;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import net.minecraft.util.Mth;
import net.pl3x.map.plugin.Logging;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;

public final class Image {
    public static final int SIZE = 512;
    private final int[][] pixels = new int[SIZE][SIZE];
    private final int maxZoom;
    private final RegionCoordinate region;
    private final Path directory;

    public Image(final RegionCoordinate region, final Path directory, final int maxZoom) {
        this.region = region;
        this.directory = directory;
        this.maxZoom = maxZoom;
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

            String fileName = scaledX + "_" + scaledZ + ".png";
            File file = new File(dir.toString(), fileName);

            try {
                BufferedImage image;
                if (file.exists()) {
                    image = ImageIO.read(file);
                } else {
                    image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
                }

                int baseX = (this.region.x() * size) & (SIZE - 1);
                int baseZ = (this.region.z() * size) & (SIZE - 1);
                for (int x = 0; x < SIZE; x += step) {
                    for (int z = 0; z < SIZE; z += step) {
                        final int rgb = this.pixels[x][z];
                        if (rgb != 0) {
                            image.setRGB(baseX + (x / step), baseZ + (z / step), rgb);
                        }
                    }
                }

                if (Config.COMPRESS_IMAGES) {
                    ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
                    writer.setOutput(ImageIO.createImageOutputStream(new FileOutputStream(file)));
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    if (param.canWriteCompressed()) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        if (param.getCompressionType() == null) {
                            param.setCompressionType(param.getCompressionTypes()[0]);
                        }
                        param.setCompressionQuality(Config.COMPRESSION_RATIO);
                    }
                    writer.write(null, new IIOImage(image, null, null), param);
                } else {
                    ImageIO.write(image, "png", file);
                }
            } catch (final IOException ex) {
                Logging.severe(
                    Lang.LOG_COULD_NOT_SAVE_REGION
                        .replace("<x>", Integer.toString(this.region.x()))
                        .replace("<z>", Integer.toString(this.region.z())),
                    ex
                );
            }
        }
    }
}
