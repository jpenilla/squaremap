package xyz.jpenilla.squaremap.common.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class Colors {
    private Colors() {
    }

    public static int removeAlpha(final int color) {
        return 0xFF << 24 | color & 0x00FFFFFF;
    }

    public static int shade(int color, int shade) {
        final float ratio = switch (shade) {
            case 0 -> 180F / 255F;
            case 1 -> 220F / 255F;
            case 2 -> 1.0F;
            default -> throw new IllegalStateException("Unexpected shade: " + shade);
        };
        return shade(color, ratio);
    }

    public static int shade(int color, float ratio) {
        int r = (int) ((color >> 16 & 0xFF) * ratio);
        int g = (int) ((color >> 8 & 0xFF) * ratio);
        int b = (int) ((color & 0xFF) * ratio);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    public static int fromNativeImage(final int rgba) {
        final int a = NativeImage.getA(rgba);
        final int r = NativeImage.getR(rgba);
        final int g = NativeImage.getG(rgba);
        final int b = NativeImage.getB(rgba);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int argbToRgba(final int color) {
        final int a = color >> 24 & 0xFF;
        final int r = color >> 16 & 0xFF;
        final int g = color >> 8 & 0xFF;
        final int b = color & 0xFF;
        return r << 24 | g << 16 | b << 8 | a;
    }

    public static int rgbaToArgb(final int color) {
        final int r = color >> 24 & 0xFF;
        final int g = color >> 16 & 0xFF;
        final int b = color >> 8 & 0xFF;
        final int a = color & 0xFF;
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int mix(int c1, int c2, float ratio) {
        if (ratio >= 1F) {
            return c2;
        } else if (ratio <= 0F) {
            return c1;
        }
        float iRatio = 1.0F - ratio;

        int r1 = c1 >> 16 & 0xFF;
        int g1 = c1 >> 8 & 0xFF;
        int b1 = c1 & 0xFF;

        int r2 = c2 >> 16 & 0xFF;
        int g2 = c2 >> 8 & 0xFF;
        int b2 = c2 & 0xFF;

        int r = (int) ((r1 * iRatio) + (r2 * ratio));
        int g = (int) ((g1 * iRatio) + (g2 * ratio));
        int b = (int) ((b1 * iRatio) + (b2 * ratio));

        return 0xFF << 24 | r << 16 | g << 8 | b;
    }

    public static int leavesMapColor() {
        return rgb(Material.LEAVES.getColor());
    }

    public static int plantsMapColor() {
        return rgb(Material.PLANT.getColor());
    }

    public static int clearMapColor() {
        return rgb(MaterialColor.NONE);
    }

    public static int parseHex(final String color) {
        final int rgba = (int) Long.parseLong(color.replace("#", ""), 16);
        return rgbaToArgb(rgba);
    }

    public static int rgb(final MaterialColor color) {
        return color.col;
    }

    public static String toHexString(final int color) {
        return String.format("#%08X", color);
    }
}
