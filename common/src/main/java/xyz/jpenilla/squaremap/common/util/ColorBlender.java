package xyz.jpenilla.squaremap.common.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class ColorBlender {
    private int a = 0;
    private int r = 0;
    private int g = 0;
    private int b = 0;
    private int count = 0;

    public void addColor(final int color) {
        this.a += (color >> 24) & 0xFF;
        this.r += (color >> 16) & 0xFF;
        this.g += (color >> 8) & 0xFF;
        this.b += color & 0xFF;
        this.count++;
    }

    public int result() {
        if (this.count == 0) {
            throw new IllegalStateException("Cannot blend 0 colors!");
        }
        final int a = this.a / this.count;
        final int r = this.r / this.count;
        final int g = this.g / this.count;
        final int b = this.b / this.count;
        return a << 24 | r << 16 | g << 8 | b;
    }

    public void reset() {
        this.a = 0;
        this.r = 0;
        this.g = 0;
        this.b = 0;
        this.count = 0;
    }
}
