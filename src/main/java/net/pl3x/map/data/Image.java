package net.pl3x.map.data;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.pl3x.map.Pl3xMap;

public class Image {
    private static final int SIZE = 512;
    private final int[][] pixels = new int[SIZE][SIZE];

    public void setPixel(int x, int z, int color) {
        this.pixels[x & (SIZE - 1)][z & (SIZE - 1)] = color;
    }

    public void save(Region region, File directory) {
        for (int zoom = 0; zoom <= 3; zoom++) {
            int step = (int) Math.pow(2, zoom);
            int size = SIZE / step;
            try {
                File dir = new File(directory, Integer.toString(3 - zoom));
                if (!dir.exists() && !dir.mkdirs()) {
                    Pl3xMap.log().severe("Could not create directory! " + dir.getAbsolutePath());
                    continue;
                }

                int scaledX = MathHelper.floor((double) region.getX() / step);
                int scaledZ = MathHelper.floor((double) region.getZ() / step);

                String fileName = scaledX + "_" + scaledZ + ".png";
                File file = new File(dir, fileName);

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
                Pl3xMap.log().severe("Could not save map for region " + region.getX() + "," + region.getZ());
                e.printStackTrace();
            }
        }
    }
}
