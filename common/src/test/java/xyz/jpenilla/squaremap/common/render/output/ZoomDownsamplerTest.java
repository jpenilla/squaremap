package xyz.jpenilla.squaremap.common.render.output;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoomDownsamplerTest {
    @Test
    void solidColorsAreUnchanged() {
        for (int value = 0; value < 256; value++) {
            final int color = 0xFF000000 | value << 16 | (255 - value) << 8 | value;
            assertEquals(color, downsample(image(2, color, color, color, color)));
        }
    }

    @Test
    void mixedColorsAverageInLinearLight() {
        // Half the light of white, which is brighter than the sRGB midpoint 0x80
        assertEquals(0xFFBCBCBC, downsample(image(2, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000)));
    }

    @Test
    void transparentPixelsSetCoverageWithoutDarkening() {
        final int red = 0xFFFF0000;
        assertEquals(0x80FF0000, downsample(image(2, red, 0, red, 0)));
        assertEquals(0x40FF0000, downsample(image(2, 0, 0, 0, red)));
        assertEquals(0, downsample(image(2, 0, 0, 0, 0)));
    }

    static BufferedImage image(final int size, final int... pixels) {
        final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, size, size, pixels, 0, size);
        return image;
    }

    private static int downsample(final BufferedImage source) {
        final BufferedImage target = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        ZoomDownsampler.downsample(source, target, 0, 0);
        return target.getRGB(0, 0);
    }
}
