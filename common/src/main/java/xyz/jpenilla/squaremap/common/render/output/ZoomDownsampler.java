package xyz.jpenilla.squaremap.common.render.output;

import java.awt.image.BufferedImage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * Halves tiles with a 2×2 box filter in linear light, weighting colors by alpha so transparent pixels don't darken edges.
 */
@DefaultQualifier(NonNull.class)
final class ZoomDownsampler {
    // sRGB channel to 16-bit linear light, and back
    private static final int[] TO_LINEAR = new int[256];
    private static final int[] FROM_LINEAR = new int[65536];

    static {
        for (int i = 0; i < TO_LINEAR.length; i++) {
            final double srgb = i / 255.0;
            final double linear = srgb <= 0.04045 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
            TO_LINEAR[i] = (int) Math.round(linear * 65535);
        }
        for (int i = 0; i < FROM_LINEAR.length; i++) {
            final double linear = i / 65535.0;
            final double srgb = linear <= 0.0031308 ? linear * 12.92 : 1.055 * Math.pow(linear, 1 / 2.4) - 0.055;
            FROM_LINEAR[i] = (int) Math.round(srgb * 255);
        }
    }

    private ZoomDownsampler() {
    }

    /**
     * Writes {@code source} at half size into {@code target}.
     *
     * @param source  image with even, equal width and height
     * @param target  destination image
     * @param targetX x of the destination area in {@code target}
     * @param targetY y of the destination area in {@code target}
     */
    static void downsample(final BufferedImage source, final BufferedImage target, final int targetX, final int targetY) {
        final int size = source.getWidth();
        final int half = size / 2;
        final int[] in = source.getRGB(0, 0, size, size, null, 0, size);
        final int[] out = new int[half * half];
        for (int y = 0; y < half; y++) {
            for (int x = 0; x < half; x++) {
                int alpha = 0;
                int red = 0;
                int green = 0;
                int blue = 0;
                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        final int argb = in[(2 * y + dy) * size + 2 * x + dx];
                        final int a = argb >>> 24;
                        alpha += a;
                        red += TO_LINEAR[(argb >> 16) & 0xFF] * a;
                        green += TO_LINEAR[(argb >> 8) & 0xFF] * a;
                        blue += TO_LINEAR[argb & 0xFF] * a;
                    }
                }
                if (alpha != 0) {
                    out[y * half + x] = (alpha + 2) / 4 << 24
                        | fromLinear(red, alpha) << 16
                        | fromLinear(green, alpha) << 8
                        | fromLinear(blue, alpha);
                }
            }
        }
        target.setRGB(targetX, targetY, half, half, out, 0, half);
    }

    private static int fromLinear(final int weighted, final int alpha) {
        return FROM_LINEAR[(weighted + alpha / 2) / alpha];
    }
}
