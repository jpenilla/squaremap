package net.pl3x.map.plugin.util;

import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.Material;
import net.minecraft.server.v1_16_R3.MaterialMapColor;
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

        int r1 = ((c1 & 0xFF0000) >> 16);
        int g1 = ((c1 & 0xFF00) >> 8);
        int b1 = (c1 & 0xFF);

        int r2 = ((c2 & 0xFF0000) >> 16);
        int g2 = ((c2 & 0xFF00) >> 8);
        int b2 = (c2 & 0xFF);

        int r = (int) ((r1 * iRatio) + (r2 * ratio));
        int g = (int) ((g1 * iRatio) + (g2 * ratio));
        int b = (int) ((b1 * iRatio) + (b2 * ratio));

        return (0xFF << 24 | r << 16 | g << 8 | b);
    }

    public static @NonNull MaterialMapColor grassMapColor() {
        return Material.GRASS.h();
    }

    public static @NonNull MaterialMapColor leavesMapColor() {
        return Material.LEAVES.h();
    }

    public static @NonNull MaterialMapColor plantsMapColor() {
        return Material.PLANT.h();
    }

    public static @NonNull MaterialMapColor waterMapColor() {
        return Material.WATER.h();
    }

    public static @NonNull MaterialMapColor blackMapColor() {
        return MaterialMapColor.b;
    }

    public static int getMapColor(final @NonNull IBlockData state) {
        final int special = SpecialColorRegistry.getColor(state);
        if (special != -1) {
            return special;
        }
        return state.d(null, null).rgb;
    }
}
