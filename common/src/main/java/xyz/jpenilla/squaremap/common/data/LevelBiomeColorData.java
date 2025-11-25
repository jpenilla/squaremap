package xyz.jpenilla.squaremap.common.data;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import xyz.jpenilla.squaremap.common.util.Colors;
import xyz.jpenilla.squaremap.common.util.Util;

public record LevelBiomeColorData(
    Reference2IntMap<Biome> grassColors,
    Reference2IntMap<Biome> foliageColors,
    Reference2IntMap<Biome> waterColors
) {
    private static int[] MAP_GRASS;
    private static int[] MAP_FOLIAGE;

    public static void loadImages(final DirectoryProvider directoryProvider) {
        final Path imagesDir = directoryProvider.webDirectory().resolve("images");
        final BufferedImage imgGrass;
        final BufferedImage imgFoliage;

        try {
            imgGrass = ImageIO.read(imagesDir.resolve("grass.png").toFile());
            imgFoliage = ImageIO.read(imagesDir.resolve("foliage.png").toFile());
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to read biome images", e);
        }

        MAP_GRASS = toArray(imgGrass);
        MAP_FOLIAGE = toArray(imgFoliage);
    }

    public static LevelBiomeColorData create(final MapWorldInternal world) {
        final Reference2IntMap<Biome> grassColors = new Reference2IntOpenHashMap<>();
        final Reference2IntMap<Biome> foliageColors = new Reference2IntOpenHashMap<>();
        final Reference2IntMap<Biome> waterColors = new Reference2IntOpenHashMap<>();

        for (final Biome biome : Util.biomeRegistry(world.serverLevel())) {
            float temperature = Mth.clamp(biome.getBaseTemperature(), 0.0F, 1.0F);
            float humidity = Mth.clamp(downfall(biome), 0.0F, 1.0F);
            grassColors.put(
                biome,
                biome.getSpecialEffects().grassColorOverride()
                    .orElse(defaultGrassColor(temperature, humidity))
                    .intValue()
            );
            foliageColors.put(
                biome,
                biome.getSpecialEffects().foliageColorOverride()
                    .orElse(Colors.mix(Colors.plantMapColor(), defaultFoliageColor(temperature, humidity), 0.85f))
                    .intValue()
            );
            waterColors.put(
                biome,
                biome.getSpecialEffects().waterColor()
            );
        }

        grassColors.putAll(world.advanced().COLOR_OVERRIDES_BIOME_GRASS);
        foliageColors.putAll(world.advanced().COLOR_OVERRIDES_BIOME_FOLIAGE);
        waterColors.putAll(world.advanced().COLOR_OVERRIDES_BIOME_WATER);

        return new LevelBiomeColorData(
            Reference2IntMaps.unmodifiable(grassColors),
            Reference2IntMaps.unmodifiable(foliageColors),
            Reference2IntMaps.unmodifiable(waterColors)
        );
    }

    private static float downfall(final Biome biome) {
        // climateSettings is the first instance field in Biome
        final Field climateSettingsField = Arrays.stream(biome.getClass().getDeclaredFields())
            .filter(it -> !Modifier.isStatic(it.getModifiers()))
            .findFirst()
            .orElseThrow();
        climateSettingsField.setAccessible(true);
        float downfall = Float.MAX_VALUE;
        // downfall() record accessor is second float returning method on Biome.ClimateSettings
        try {
            final Object climateSettings = climateSettingsField.get(biome);
            int count = 0;
            for (final Method m : climateSettings.getClass().getDeclaredMethods()) {
                if (m.getReturnType() == Float.TYPE) {
                    count++;
                    if (count == 2) {
                        downfall = (float) m.invoke(climateSettings);
                        break;
                    }
                }
            }
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        if (downfall == Float.MAX_VALUE) {
            throw new IllegalStateException("Could not determine 'downfall' for biome: " + biome);
        }
        return downfall;
    }

    private static int[] toArray(final BufferedImage image) {
        final int[] array = new int[256 * 256];
        for (int x = 0; x < 256; ++x) {
            for (int y = 0; y < 256; ++y) {
                final int color = image.getRGB(x, y);
                final int r = color >> 16 & 0xFF;
                final int g = color >> 8 & 0xFF;
                final int b = color & 0xFF;
                array[x + y * 256] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return array;
    }

    private static int defaultGrassColor(final double temperature, final double humidity) {
        int j = (int) ((1.0 - (humidity * temperature)) * 255.0);
        int i = (int) ((1.0 - temperature) * 255.0);
        int k = j << 8 | i;
        if (k > MAP_GRASS.length) {
            return 0;
        }
        return MAP_GRASS[k];
    }

    private static int defaultFoliageColor(final double temperature, final double humidity) {
        int i = (int) ((1.0 - temperature) * 255.0);
        int j = (int) ((1.0 - (humidity * temperature)) * 255.0);
        return MAP_FOLIAGE[(j << 8 | i)];
    }
}
