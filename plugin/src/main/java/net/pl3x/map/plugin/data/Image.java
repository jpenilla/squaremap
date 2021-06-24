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
import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;

public class Image {
    public static final int SIZE = 512;
    private final int[][] pixels = new int[SIZE][SIZE];
    private final int maxZoom;
    private final Region region;
    private final Path directory;

    public Image(Region region, Path directory, int maxZoom) {
        this.region = region;
        this.directory = directory;
        this.maxZoom = maxZoom;
    }

    public synchronized void setPixel(int x, int z, int color) {
        this.pixels[x & (SIZE - 1)][z & (SIZE - 1)] = color;
    }

    public void save() {
        for (int zoom = 0; zoom <= maxZoom; zoom++) {
            Path dir = Path.of(directory.toString(), Integer.toString(maxZoom - zoom));
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                Logger.severe(Lang.LOG_COULD_NOT_CREATE_DIR
                        .replace("{path}", dir.toAbsolutePath().toString()), e);
                continue;
            }

            int step = (int) Math.pow(2, zoom);
            int size = SIZE / step;
            int scaledX = Mth.floor((double) region.getX() / step);
            int scaledZ = Mth.floor((double) region.getZ() / step);

            String fileName = scaledX + "_" + scaledZ + ".png";
            File file = new File(dir.toString(), fileName);

            try {
                BufferedImage image;
                if (file.exists()) {
                    image = ImageIO.read(file);
                } else {
                    image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
                }

                int baseX = (region.getX() * size) & (SIZE - 1);
                int baseZ = (region.getZ() * size) & (SIZE - 1);
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
            } catch (IOException e) {
                Logger.severe(Lang.LOG_COULD_NOT_SAVE_REGION
                        .replace("{x}", Integer.toString(region.getX()))
                        .replace("{z}", Integer.toString(region.getZ())));
                e.printStackTrace();
            }
        }
    }
}
