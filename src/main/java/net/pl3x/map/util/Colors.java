package net.pl3x.map.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.server.v1_16_R3.BiomeBase;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.IRegistryWritable;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.World;
import net.pl3x.map.task.FullRender;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftNamespacedKey;

public class Colors {
    private final Map<BiomeBase, Integer> grassColors = new HashMap<>();
    private final Map<BiomeBase, Integer> foliageColors = new HashMap<>();
    private final Map<BiomeBase, Integer> waterColors = new HashMap<>();

    private final BiomeBase swamp;
    private final BiomeBase swampHills;
    private final BiomeBase darkForest;
    private final BiomeBase darkForestHills;

    public Colors(World world) {
        IRegistryWritable<BiomeBase> biomeRegistry = world.r().b(IRegistry.ay);
        swamp = biomeRegistry.get(CraftNamespacedKey.toMinecraft(Biome.SWAMP.getKey()));
        swampHills = biomeRegistry.get(CraftNamespacedKey.toMinecraft(Biome.SWAMP_HILLS.getKey()));
        darkForest = biomeRegistry.get(CraftNamespacedKey.toMinecraft(Biome.DARK_FOREST.getKey()));
        darkForestHills = biomeRegistry.get(CraftNamespacedKey.toMinecraft(Biome.DARK_FOREST_HILLS.getKey()));

        BufferedImage imgGrass, imgFoliage;
        try {
            File imagesDir = new File(FileUtil.getWebFolder(), "images");
            imgGrass = ImageIO.read(new File(imagesDir, "grass.png"));
            imgFoliage = ImageIO.read(new File(imagesDir, "foliage.png"));

            int[] mapGrass = init(imgGrass);
            int[] mapFoliage = init(imgFoliage);

            for (BiomeBase biome : biomeRegistry) {
                float temperature = MathHelper.a(biome.k(), 0.0F, 1.0F);
                float humidity = MathHelper.a(biome.getHumidity(), 0.0F, 1.0F);
                grassColors.put(biome, getGrassColor(mapGrass, temperature, humidity));
                foliageColors.put(biome, getFoliageColor(mapFoliage, temperature, humidity));
                waterColors.put(biome, shade(FullRender.BLUE.rgb, 0.75F + (temperature * 0.25F)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getGrassColor(BiomeBase biome, BlockPosition.MutableBlockPosition pos) {
        Integer color = grassColors.get(biome);
        if (color != null) {
            color = getModifiedGrassColor(biome, pos, color, 1.0F);
        } else {
            color = 0;
        }
        return color;
    }

    public int getFoliageColor(BiomeBase biome, BlockPosition.MutableBlockPosition pos) {
        Integer color = foliageColors.get(biome);
        if (color != null) {
            color = getModifiedGrassColor(biome, pos, color, 0.9F);
        } else {
            color = 0;
        }
        return color;
    }

    public int getWaterColor(BiomeBase biome, BlockPosition.MutableBlockPosition pos) {
        Integer color = waterColors.get(biome);
        if (color != null) {
            color = getModifiedGrassColor(biome, pos, color, 0.8F);
        } else {
            color = 0;
        }
        return color;
    }

    private int getModifiedGrassColor(BiomeBase biome, BlockPosition.MutableBlockPosition pos, int color, float shade) {
        if (biome == swamp || biome == swampHills) {
            double f = BiomeBase.f.a(pos.getX() * 0.0225, pos.getZ() * 0.0225, false);
            if (f < -0.1) {
                return mix(color, shade(5011004, shade), 0.75F);
            }
            return mix(color, shade(6975545, shade), 0.75F);
        } else if (biome == darkForest || biome == darkForestHills) {
            return (color & 0xFEFEFE) + 2634762 >> 1;
        } else {
            return color;
        }
    }

    private int[] init(BufferedImage image) {
        int[] map = new int[256 * 256];
        for (int x = 0; x < 256; ++x) {
            for (int y = 0; y < 256; ++y) {
                int color = image.getRGB(x, y);
                int r = color >> 16 & 0xFF;
                int g = color >> 8 & 0xFF;
                int b = color & 0xFF;
                map[x + y * 256] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return map;
    }

    private int mix(int c1, int c2, float ratio) {
        if (ratio > 1f) ratio = 1f;
        else if (ratio < 0f) ratio = 0f;
        float iRatio = 1.0f - ratio;

        int r1 = ((c1 & 0xff0000) >> 16);
        int g1 = ((c1 & 0xff00) >> 8);
        int b1 = (c1 & 0xff);

        int r2 = ((c2 & 0xff0000) >> 16);
        int g2 = ((c2 & 0xff00) >> 8);
        int b2 = (c2 & 0xff);

        int r = (int) ((r1 * iRatio) + (r2 * ratio));
        int g = (int) ((g1 * iRatio) + (g2 * ratio));
        int b = (int) ((b1 * iRatio) + (b2 * ratio));

        return (0xFF << 24 | r << 16 | g << 8 | b);
    }

    private int getGrassColor(int[] map, double temperature, double humidity) {
        int j = (int) ((1.0 - (humidity * temperature)) * 255.0);
        int i = (int) ((1.0 - temperature) * 255.0);
        int k = j << 8 | i;
        if (k > map.length) {
            return 0;
        }
        return map[k];
    }

    private int getFoliageColor(int[] map, double temperature, double humidity) {
        int i = (int) ((1.0 - temperature) * 255.0);
        int j = (int) ((1.0 - (humidity * temperature)) * 255.0);
        return map[(j << 8 | i)];
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
}
