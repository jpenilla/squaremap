package net.pl3x.map.util;

import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.MaterialMapColor;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;

public class Colors {
    public static int[] COLOR = new int[MaterialMapColor.a.length * 2];

    static {
        for (int i = 0; i < MaterialMapColor.a.length; i++) {
            if (MaterialMapColor.a[i] != null) {
                COLOR[i] = (0xFF << 24) + MaterialMapColor.a[i].rgb;
                COLOR[i + MaterialMapColor.a.length] = darker(COLOR[i]);
            }
        }
    }

    private static int darker(int color) {
        float ratio = 1.0f - 0.1f;
        int r = (int) (((color >> 16) & 0xFF) * ratio);
        int g = (int) (((color >> 8) & 0xFF) * ratio);
        int b = (int) ((color & 0xFF) * ratio);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    public static int getColor(Chunk chunk, int x, int z) {
        Block block = chunk.getWorld().getHighestBlockAt(x, z, HeightMap.WORLD_SURFACE);
        IBlockData state = ((CraftBlockData) block.getBlockData()).getState();
        int index = state.d(null, null).aj;
        if (block.getY() % 2 != 0) {
            index += MaterialMapColor.a.length;
        }
        return COLOR[index];
    }
}
