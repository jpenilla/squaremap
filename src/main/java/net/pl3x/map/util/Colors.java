package net.pl3x.map.util;

import net.minecraft.server.v1_16_R3.Material;
import net.minecraft.server.v1_16_R3.MaterialMapColor;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class Colors {
    private Colors() {
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

    public static @NonNull MaterialMapColor grassMapColor() {
        return Material.GRASS.h();
    }

    public static @NonNull MaterialMapColor foliageMapColor() {
        return Material.LEAVES.h();
    }

    public static @NonNull MaterialMapColor blueMapColor() {
        return Material.WATER.h();
    }

    public static @NonNull MaterialMapColor blackMapColor() {
        return MaterialMapColor.b;
    }
}
