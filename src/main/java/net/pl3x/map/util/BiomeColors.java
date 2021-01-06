package net.pl3x.map.util;

import net.minecraft.server.v1_16_R3.BiomeBase;
import net.minecraft.server.v1_16_R3.BiomeFog;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.IRegistryWritable;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.World;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class BiomeColors {
    private final Map<BiomeBase, Integer> grassColors = new HashMap<>();
    private final Map<BiomeBase, Integer> foliageColors = new HashMap<>();

    public BiomeColors(World world) {
        IRegistryWritable<BiomeBase> biomeRegistry = world.r().b(IRegistry.ay);

        BufferedImage imgGrass, imgFoliage;
        try {
            File imagesDir = FileUtil.WEB_DIR.resolve("images").toFile();
            imgGrass = ImageIO.read(new File(imagesDir, "grass.png"));
            imgFoliage = ImageIO.read(new File(imagesDir, "foliage.png"));

            int[] mapGrass = init(imgGrass);
            int[] mapFoliage = init(imgFoliage);

            for (BiomeBase biome : biomeRegistry) {
                float temperature = MathHelper.a(biome.k(), 0.0F, 1.0F);
                float humidity = MathHelper.a(biome.getHumidity(), 0.0F, 1.0F);
                grassColors.put(biome, getGrassColor(mapGrass, temperature, humidity));
                foliageColors.put(biome, getFoliageColor(mapFoliage, temperature, humidity));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int grass(final @NonNull BiomeBase biome, final @NonNull BlockPosition blockPosition) {
        return modifiedGrassColor(
                biome,
                blockPosition,
                BiomeEffectsReflection.grassColor(biome).orElse(this.grassColors.get(biome)),
                1.0f
        );
    }

    public int foliage(final @NonNull BiomeBase biome, final @NonNull BlockPosition blockPosition) {
        return BiomeEffectsReflection.foliageColor(biome).orElse(this.foliageColors.get(biome));
    }

    public static int water(final @NonNull BiomeBase biome, final @NonNull BlockPosition blockPosition) {
        return BiomeEffectsReflection.waterColor(biome);
    }

    private static int modifiedGrassColor(final @NonNull BiomeBase biome, final @NonNull BlockPosition pos, final int color, final float shade) {
        final String modifier = BiomeEffectsReflection.grassColorModifier(biome).getName().toUpperCase(Locale.ENGLISH); // As of 1.16.4: Can be SWAMP, DARK_FOREST, or NONE
        switch (modifier) {
            case "NONE":
                return color;
            case "SWAMP":
                double f = BiomeBase.f.a(pos.getX() * 0.0225, pos.getZ() * 0.0225, false); // wtf is this for?
                if (f < -0.1) {
                    return mix(color, Colors.shade(5011004, shade), 0.75F);
                }
                return mix(color, Colors.shade(6975545, shade), 0.75F);
            case "DARK_FOREST":
                return (color & 0xFEFEFE) + 2634762 >> 1;
            default:
                throw new IllegalArgumentException("Unknown or invalid grass color modifier: " + modifier);
        }
    }

    private static int mix(final int c1, final int c2, float ratio) {
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

    // Utils for reflecting into BiomeFog/BiomeEffects
    private static final class BiomeEffectsReflection {
        private BiomeEffectsReflection() {
        }

        private static final Field grass_color = ReflectionUtil.needField(BiomeFog.class, "g");
        private static final Field foliage_color = ReflectionUtil.needField(BiomeFog.class, "f");
        private static final Field water_color = ReflectionUtil.needField(BiomeFog.class, "c");
        private static final Field grass_color_modifier = ReflectionUtil.needField(BiomeFog.class, "h");

        @SuppressWarnings("unchecked")
        private static @NonNull Optional<Integer> grassColor(final @NonNull BiomeBase biome) {
            try {
                return (Optional<Integer>) grass_color.get(biomeEffects(biome));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Could not find grass color", e);
            }
        }

        @SuppressWarnings("unchecked")
        private static @NonNull Optional<Integer> foliageColor(final @NonNull BiomeBase biome) {
            try {
                return (Optional<Integer>) foliage_color.get(biomeEffects(biome));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Could not find foliage color", e);
            }
        }

        private static BiomeFog.@NonNull GrassColor grassColorModifier(final @NonNull BiomeBase biome) {
            try {
                return (BiomeFog.GrassColor) grass_color_modifier.get(biomeEffects(biome));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Could not find grass color modifier", e);
            }
        }

        private static int waterColor(final @NonNull BiomeBase biome) {
            try {
                return water_color.getInt(biomeEffects(biome));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Could not find water color", e);
            }
        }

        private static @NonNull BiomeFog biomeEffects(final @NonNull BiomeBase biome) {
            return biome.l();
        }
    }
}
