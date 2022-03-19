package xyz.jpenilla.squaremap.common.data;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.util.Colors;
import xyz.jpenilla.squaremap.common.util.Util;

public record LevelBiomeColorData(
    Reference2IntMap<Biome> grassColors,
    Reference2IntMap<Biome> foliageColors,
    Reference2IntMap<Biome> waterColors
) {
    private static final int[] MAP_GRASS;
    private static final int[] MAP_FOLIAGE;

    static {
        final Path imagesDir = SquaremapCommon.instance().injector().getInstance(DirectoryProvider.class)
            .webDirectory().resolve("images");
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
            float humidity = Mth.clamp(biome.getDownfall(), 0.0F, 1.0F);
            grassColors.put(
                biome,
                biome.getSpecialEffects().getGrassColorOverride()
                    .orElse(defaultGrassColor(temperature, humidity))
                    .intValue()
            );
            foliageColors.put(
                biome,
                biome.getSpecialEffects().getFoliageColorOverride()
                    .orElse(Colors.mix(Colors.leavesMapColor(), defaultFoliageColor(temperature, humidity), 0.85f))
                    .intValue()
            );
            waterColors.put(
                biome,
                biome.getSpecialEffects().getWaterColor()
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

    private static int defaultGrassColor(double temperature, double humidity) {
        int j = (int) ((1.0 - (humidity * temperature)) * 255.0);
        int i = (int) ((1.0 - temperature) * 255.0);
        int k = j << 8 | i;
        if (k > MAP_GRASS.length) {
            return 0;
        }
        return MAP_GRASS[k];
    }

    private static int defaultFoliageColor(double temperature, double humidity) {
        int i = (int) ((1.0 - temperature) * 255.0);
        int j = (int) ((1.0 - (humidity * temperature)) * 255.0);
        return MAP_FOLIAGE[(j << 8 | i)];
    }
}
