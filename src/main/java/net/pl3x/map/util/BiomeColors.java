package net.pl3x.map.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import net.minecraft.server.v1_16_R3.BiomeBase;
import net.minecraft.server.v1_16_R3.BiomeFog;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.IRegistryWritable;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.World;
import net.pl3x.map.configuration.WorldConfig;

public final class BiomeColors {
    private final Cache<Long, BiomeBase> CACHE = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.SECONDS).maximumSize(100000L).build();

    private final World world;
    private final WorldConfig worldConfig;

    private final Map<BiomeBase, Integer> grassColors = new HashMap<>();
    private final Map<BiomeBase, Integer> foliageColors = new HashMap<>();
    private final Map<BiomeBase, Integer> waterColors = new HashMap<>();

    private final BlockPosition.MutableBlockPosition pos1 = new BlockPosition.MutableBlockPosition();

    public BiomeColors(World world) {
        this.world = world;
        this.worldConfig = WorldConfig.get(world.getWorld());

        BufferedImage imgGrass, imgFoliage;
        try {
            File imagesDir = FileUtil.WEB_DIR.resolve("images").toFile();
            imgGrass = ImageIO.read(new File(imagesDir, "grass.png"));
            imgFoliage = ImageIO.read(new File(imagesDir, "foliage.png"));

            int[] mapGrass = init(imgGrass);
            int[] mapFoliage = init(imgFoliage);

            IRegistryWritable<BiomeBase> biomeRegistry = world.r().b(IRegistry.ay);
            for (BiomeBase biome : biomeRegistry) {
                float temperature = MathHelper.a(biome.k(), 0.0F, 1.0F);
                float humidity = MathHelper.a(biome.getHumidity(), 0.0F, 1.0F);
                grassColors.put(biome, BiomeEffectsReflection.grassColor(biome)
                        .orElse(getDefaultGrassColor(mapGrass, temperature, humidity)));
                foliageColors.put(biome, BiomeEffectsReflection.foliageColor(biome)
                        .orElse(getDefaultFoliageColor(mapFoliage, temperature, humidity)));
                waterColors.put(biome, BiomeEffectsReflection.waterColor(biome));
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private int getDefaultGrassColor(int[] map, double temperature, double humidity) {
        int j = (int) ((1.0 - (humidity * temperature)) * 255.0);
        int i = (int) ((1.0 - temperature) * 255.0);
        int k = j << 8 | i;
        if (k > map.length) {
            return 0;
        }
        return map[k];
    }

    private int getDefaultFoliageColor(int[] map, double temperature, double humidity) {
        int i = (int) ((1.0 - temperature) * 255.0);
        int j = (int) ((1.0 - (humidity * temperature)) * 255.0);
        return map[(j << 8 | i)];
    }

    public int grass(BiomeBase biome, BlockPosition pos, boolean smooth) {
        return smooth ? smooth(pos, grassColors) : grassColors.get(biome);
    }

    public int foliage(BiomeBase biome, BlockPosition pos, boolean smooth) {
        return smooth ? smooth(pos, foliageColors) : foliageColors.get(biome);
    }

    public int water(BiomeBase biome, BlockPosition pos, boolean smooth) {
        return smooth ? smooth(pos, waterColors) : waterColors.get(biome);
    }

    private int smooth(BlockPosition pos, Map<BiomeBase, Integer> colorSampler) {
        int radius = worldConfig.MAP_WATER_BIOMES_BLEND;
        int rgb, r = 0, g = 0, b = 0, count = 0;
        for (int x = pos.getX() - radius; x < pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z < pos.getZ() + radius; z++) {
                pos1.setValues(x, pos.getY(), z);
                BiomeBase biome = getBiome(pos1);
                rgb = colorSampler.get(biome);
                r += (rgb >> 16) & 0xFF;
                g += (rgb >> 8) & 0xFF;
                b += rgb & 0xFF;
                count++;
            }
        }
        rgb = r / count;
        rgb = (rgb << 8) + g / count;
        rgb = (rgb << 8) + b / count;
        return rgb;
    }

    public BiomeBase getBiome(BlockPosition pos) {
        long hash = (long) pos.getX() << 32 | pos.getZ() & 0xffffffffL;
        BiomeBase biome = CACHE.getIfPresent(hash);
        if (biome == null) {
            pos1.setValues(pos.getX(), pos.getY(), pos.getZ());
            biome = world.getBiome(pos);
            CACHE.put(hash, biome);
        }
        return biome;
    }

    public static int modifiedGrassColor(BiomeBase biome, BlockPosition pos, int color) {
        BiomeFog.GrassColor modifier = BiomeEffectsReflection.grassColorModifier(biome);
        switch (modifier) {
            case NONE:
                return color;
            case SWAMP:
                // swamps have 2 grass colors, depends on sample from noise generator
                double sample = BiomeBase.f.a(pos.getX() * 0.0225, pos.getZ() * 0.0225, false);
                if (sample < -0.1) {
                    return 5011004;
                }
                return 6975545;
            case DARK_FOREST:
                return (color & 0xFEFEFE) + 2634762 >> 1;
            default:
                throw new IllegalArgumentException("Unknown or invalid grass color modifier: " + modifier.getName());
        }
    }

    // Utils for reflecting into BiomeFog/BiomeEffects
    private static final class BiomeEffectsReflection {
        private BiomeEffectsReflection() {
        }

        private static final Field grass_color = ReflectionUtil.needField(BiomeFog.class, "g");
        private static final Field foliage_color = ReflectionUtil.needField(BiomeFog.class, "f");
        private static final Field water_color = ReflectionUtil.needField(BiomeFog.class, "c");
        private static final Field grass_color_modifier = ReflectionUtil.needField(BiomeFog.class, "h");

        private static Optional<Integer> grassColor(BiomeBase biome) {
            try {
                //noinspection unchecked
                return (Optional<Integer>) grass_color.get(biomeEffects(biome));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Could not find grass color", e);
            }
        }

        private static Optional<Integer> foliageColor(BiomeBase biome) {
            try {
                //noinspection unchecked
                return (Optional<Integer>) foliage_color.get(biomeEffects(biome));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Could not find foliage color", e);
            }
        }

        private static BiomeFog.GrassColor grassColorModifier(BiomeBase biome) {
            try {
                return (BiomeFog.GrassColor) grass_color_modifier.get(biomeEffects(biome));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Could not find grass color modifier", e);
            }
        }

        private static int waterColor(BiomeBase biome) {
            try {
                return water_color.getInt(biomeEffects(biome));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Could not find water color", e);
            }
        }

        private static BiomeFog biomeEffects(BiomeBase biome) {
            return biome.l();
        }
    }
}
