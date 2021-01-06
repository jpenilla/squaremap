package net.pl3x.map.data;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.pl3x.map.Logger;
import net.pl3x.map.configuration.Lang;

public class Image {
    public static final int MAX_ZOOM = 3;
    public static final int SIZE = 512;
    private final int[][] pixels = new int[SIZE][SIZE];

    public void setPixel(int x, int z, int color) {
        this.pixels[x & (SIZE - 1)][z & (SIZE - 1)] = color;
    }

    public void save(Region region, Path directory) {
        for (int zoom = 0; zoom <= MAX_ZOOM; zoom++) {
            Path dir = Path.of(directory.toString(), Integer.toString(MAX_ZOOM - zoom));
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                Logger.severe(Lang.LOG_COULD_NOT_CREATE_DIR
                        .replace("{path}", dir.toAbsolutePath().toString()));
                continue;
            }

            int step = (int) Math.pow(2, zoom);
            int size = SIZE / step;
            int scaledX = MathHelper.floor((double) region.getX() / step);
            int scaledZ = MathHelper.floor((double) region.getZ() / step);

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
                        image.setRGB(baseX + (x / step), baseZ + (z / step), this.pixels[x][z]);
                    }
                }

                ImageIO.write(image, "png", file);
            } catch (IOException e) {
                Logger.severe(Lang.LOG_COULD_NOT_SAVE_REGION
                        .replace("{x}", Integer.toString(region.getX()))
                        .replace("{z}", Integer.toString(region.getZ())));
                e.printStackTrace();
            }
        }
    }
}
