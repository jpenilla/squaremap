package xyz.jpenilla.squaremap.common.data;

import java.awt.Color;
import java.util.Arrays;
import net.minecraft.util.Mth;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class Image {
    private static final int TRANSPARENT = new Color(0, 0, 0, 0).getRGB();
    public static final int SIZE = 512;
    private final RegionCoordinate region;
    private final int maxZoom;
    private int @Nullable [][] pixels = null;

    public Image(final RegionCoordinate region, final int maxZoom) {
        this.region = region;
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

    public synchronized void drawTo(final TileCache cache) {
        final int @Nullable [][] pixels = this.pixels;
        if (pixels == null) {
            return;
        }

        for (int zoom = 0; zoom <= this.maxZoom; zoom++) {
            final int step = (int) Math.pow(2, zoom);
            final int size = SIZE / step;
            final int fileZoom = this.maxZoom - zoom;
            final int scaledX = Mth.floor((double) this.region.x() / step);
            final int scaledZ = Mth.floor((double) this.region.z() / step);

            final int baseX = (this.region.x() * size) & (SIZE - 1);
            final int baseZ = (this.region.z() * size) & (SIZE - 1);

            // the most detailed zoom level holds one tile per region, so no other region ever
            // draws into it and there is nothing to gain from keeping it in memory
            final boolean retain = fileZoom != this.maxZoom;

            cache.draw(new TileCoordinate(fileZoom, scaledX, scaledZ), retain, image -> {
                for (int x = 0; x < SIZE; x += step) {
                    for (int z = 0; z < SIZE; z += step) {
                        final int pixel = pixels[x][z];
                        if (pixel != Integer.MIN_VALUE) {
                            final int color = pixel == 0 ? TRANSPARENT : pixel;
                            image.setRGB(baseX + (x / step), baseZ + (z / step), color);
                        }
                    }
                }
            });
        }
    }
}
