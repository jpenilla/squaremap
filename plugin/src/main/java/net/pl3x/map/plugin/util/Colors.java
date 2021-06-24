package net.pl3x.map.plugin.util;

import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class Colors {
    private Colors() {
    }

    public static int removeAlpha(final int color) {
        return 0xFF << 24 | color & 0x00FFFFFF;
    }

    public static int shade(int color, int shade) {
        float ratio = 220F / 255F;
        if (shade == 2) ratio = 1.0F;
        if (shade == 0) ratio = 180F / 255F;
        return shade(color, ratio);
    }

    public static int shade(int color, float ratio) {
        int r = (int) ((color >> 16 & 0xFF) * ratio);
        int g = (int) ((color >> 8 & 0xFF) * ratio);
        int b = (int) ((color & 0xFF) * ratio);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    public static int mix(int c1, int c2, float ratio) {
        if (ratio >= 1F) return c2;
        else if (ratio <= 0F) return c1;
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

        return (0xFF << 24 | r << 16 | g << 8 | b);
    }

    public static int grassMapColor() {
        return rgb(Material.GRASS.getColor());
    }

    public static int leavesMapColor() {
        return rgb(Material.LEAVES.getColor());
    }

    public static int plantsMapColor() {
        return rgb(Material.PLANT.getColor());
    }

    public static int waterMapColor() {
        return rgb(Material.WATER.getColor());
    }

    public static int clearMapColor() {
        return rgb(MaterialColor.NONE);
    }

    public static Integer parseHex(final @NonNull String color) {
        return (int) Long.parseLong(color.replace("#", ""), 16);
    }

    public static int rgb(MaterialColor color) {
        return color.col;
    }
}
