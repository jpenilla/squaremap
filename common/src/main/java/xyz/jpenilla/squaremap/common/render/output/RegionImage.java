package xyz.jpenilla.squaremap.common.render.output;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.coordinate.RegionCoordinate;
import xyz.jpenilla.squaremap.common.coordinate.TileCoordinate;

@DefaultQualifier(NonNull.class)
public final class RegionImage {
    private final TileCoordinate tile;
    private volatile int @Nullable [][] pixels = null;

    public RegionImage(final RegionCoordinate region, final int maxZoom) {
        this.tile = new TileCoordinate(maxZoom, region.x(), region.z());
    }

    // Callers write distinct pixels, so only the allocation needs the lock.
    public void setPixel(final int x, final int z, final int color) {
        int @Nullable [][] pixels = this.pixels;
        if (pixels == null) {
            pixels = this.allocatePixels();
        }
        pixels[x & (TileImages.SIZE - 1)][z & (TileImages.SIZE - 1)] = color;
    }

    private synchronized int[][] allocatePixels() {
        int @Nullable [][] pixels = this.pixels;
        if (pixels == null) {
            pixels = new int[TileImages.SIZE][TileImages.SIZE];
            for (final int[] arr : pixels) {
                Arrays.fill(arr, Integer.MIN_VALUE);
            }
            this.pixels = pixels;
        }
        return pixels;
    }

    // The max zoom tile, which covers exactly this region
    TileCoordinate tile() {
        return this.tile;
    }

    boolean isEmpty() {
        return this.pixels == null;
    }

    // Draws the rendered pixels over the region's max zoom tile image, leaving the rest unchanged
    void drawInto(final BufferedImage image) {
        final int[][] pixels = this.pixels;
        for (int x = 0; x < TileImages.SIZE; x++) {
            for (int z = 0; z < TileImages.SIZE; z++) {
                final int pixel = pixels[x][z];
                if (pixel != Integer.MIN_VALUE) {
                    image.setRGB(x, z, pixel);
                }
            }
        }
    }
}
