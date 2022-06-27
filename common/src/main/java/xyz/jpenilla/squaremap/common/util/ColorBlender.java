package xyz.jpenilla.squaremap.common.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class ColorBlender {
    private int r = 0;
    private int g = 0;
    private int b = 0;
    private int count = 0;

    public void addColor(final int color) {
        this.r += (color >> 16) & 0xFF;
        this.g += (color >> 8) & 0xFF;
        this.b += color & 0xFF;
        this.count++;
    }

    public int result() {
        if (this.count == 0) {
            throw new IllegalStateException("Cannot blend 0 colors!");
        }
        int rgb = this.r / this.count;
        rgb = (rgb << 8) + this.g / this.count;
        rgb = (rgb << 8) + this.b / this.count;
        return rgb;
    }

    public void reset() {
        this.r = 0;
        this.g = 0;
        this.b = 0;
        this.count = 0;
    }
}
