package net.pl3x.map.plugin.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import net.minecraft.server.v1_16_R3.BiomeBase;
import net.minecraft.server.v1_16_R3.BiomeFog;
import net.minecraft.server.v1_16_R3.Biomes;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.Material;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.World;
import net.pl3x.map.plugin.configuration.WorldConfig;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public final class BiomeColors {
    private static final Set<Block> grassColorBlocks = ImmutableSet.of(
            Blocks.GRASS_BLOCK,
            Blocks.GRASS,
            Blocks.TALL_GRASS,
            Blocks.FERN,
            Blocks.LARGE_FERN,
            Blocks.POTTED_FERN,
            Blocks.SUGAR_CANE
    );

    private static final Set<Block> foliageColorBlocks = ImmutableSet.of(
            Blocks.VINE,
            Blocks.OAK_LEAVES,
            Blocks.JUNGLE_LEAVES,
            Blocks.ACACIA_LEAVES,
            Blocks.DARK_OAK_LEAVES
    );

    private static final Set<Block> waterColorBlocks = ImmutableSet.of(
            Blocks.WATER,
            Blocks.BUBBLE_COLUMN,
            Blocks.CAULDRON
    );

    private static final Set<Material> waterColorMaterials = ImmutableSet.of(
            Material.WATER_PLANT,
            Material.REPLACEABLE_WATER_PLANT
    );

    private static final int[] mapGrass;
    private static final int[] mapFoliage;

    static {
        final File imagesDir = FileUtil.WEB_DIR.resolve("images").toFile();
        final BufferedImage imgGrass;
        final BufferedImage imgFoliage;
        try {
            imgGrass = ImageIO.read(new File(imagesDir, "grass.png"));
            imgFoliage = ImageIO.read(new File(imagesDir, "foliage.png"));
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to read biome images", e);
        }

        mapGrass = init(imgGrass);
        mapFoliage = init(imgFoliage);
    }

    private final Cache<Long, BiomeBase> blockPosBiomeCache = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.SECONDS).maximumSize(100000L).build();

    private final World world;
    private final WorldConfig worldConfig;

    private final Map<BiomeBase, Integer> grassColors = new HashMap<>();
    private final Map<BiomeBase, Integer> foliageColors = new HashMap<>();
    private final Map<BiomeBase, Integer> waterColors = new HashMap<>();

    private final BlockPosition.MutableBlockPosition sharedBlockPos = new BlockPosition.MutableBlockPosition();

    private BiomeColors(final @NonNull World world) {
        this.world = world;
        this.worldConfig = WorldConfig.get(world.getWorld());

        final IRegistry<BiomeBase> biomeRegistry = this.getBiomeRegistry();
        for (final BiomeBase biome : biomeRegistry) {
            float temperature = MathHelper.a(biome.k(), 0.0F, 1.0F);
            float humidity = MathHelper.a(biome.getHumidity(), 0.0F, 1.0F);
            grassColors.put(biome, BiomeEffectsReflection.grassColor(biome)
                    .orElse(getDefaultGrassColor(temperature, humidity)));
            foliageColors.put(biome, BiomeEffectsReflection.foliageColor(biome)
                    .orElse(Colors.mix(Colors.leavesMapColor().rgb, getDefaultFoliageColor(temperature, humidity), 0.85f)));
            waterColors.put(biome, BiomeEffectsReflection.waterColor(biome));
        }

        final int darkForestColor = (Colors.leavesMapColor().rgb & 0xFEFEFE) + 2634762 >> 1;
        final BiomeBase DARK_FOREST = biomeRegistry.get(Biomes.DARK_FOREST.a());
        final BiomeBase DARK_FOREST_HILLS = biomeRegistry.get(Biomes.DARK_FOREST_HILLS.a());
        foliageColors.put(DARK_FOREST, BiomeEffectsReflection.foliageColor(DARK_FOREST).orElse(darkForestColor));
        foliageColors.put(DARK_FOREST_HILLS, BiomeEffectsReflection.foliageColor(DARK_FOREST_HILLS).orElse(darkForestColor));
    }

    public static @NonNull BiomeColors forWorld(final @NonNull World world) {
        return new BiomeColors(world);
    }

    public int modifyColorFromBiome(int color, final @NonNull Chunk chunk, final @NonNull BlockPosition pos) {
        final IBlockData data = chunk.getType(pos);
        final Material mat = data.getMaterial();
        final Block block = data.getBlock();

        if (grassColorBlocks.contains(block)) {
            color = this.grass(pos);
        } else if (foliageColorBlocks.contains(block)) {
            color = this.foliage(pos);
        } else if (waterColorBlocks.contains(block) || waterColorMaterials.contains(mat)) {
            int modColor = water(pos);
            color = Colors.mix(color, modColor, 0.8F);
        }

        return color;
    }

    private static int @NonNull [] init(final @NonNull BufferedImage image) {
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

    private static int getDefaultGrassColor(double temperature, double humidity) {
        int j = (int) ((1.0 - (humidity * temperature)) * 255.0);
        int i = (int) ((1.0 - temperature) * 255.0);
        int k = j << 8 | i;
        if (k > mapGrass.length) {
            return 0;
        }
        return mapGrass[k];
    }

    private static int getDefaultFoliageColor(double temperature, double humidity) {
        int i = (int) ((1.0 - temperature) * 255.0);
        int j = (int) ((1.0 - (humidity * temperature)) * 255.0);
        return mapFoliage[(j << 8 | i)];
    }

    private int grass(final @NonNull BlockPosition pos) {
        if (this.worldConfig.MAP_BIOMES_BLEND > 0) {
            return this.sampleNeighbors(pos, this.worldConfig.MAP_BIOMES_BLEND, this::grassColorSampler);
        }
        return this.grassColorSampler(this.getBiomeWithCaching(pos), pos);
    }

    private int grassColorSampler(final @NonNull BiomeBase biome, final @NonNull BlockPosition pos) {
        return modifiedGrassColor(biome, pos, this.grassColors.get(biome));
    }

    private int foliage(final @NonNull BlockPosition pos) {
        if (this.worldConfig.MAP_BIOMES_BLEND > 0) {
            return this.sampleNeighbors(pos, this.worldConfig.MAP_BIOMES_BLEND, (biome, b) -> this.foliageColors.get(biome));
        }
        return this.foliageColors.get(this.getBiomeWithCaching(pos));
    }

    private int water(final @NonNull BlockPosition pos) {
        if (this.worldConfig.MAP_BIOMES_BLEND > 0) {
            return this.sampleNeighbors(pos, this.worldConfig.MAP_BIOMES_BLEND, (biome, b) -> this.waterColors.get(biome));
        }
        return this.waterColors.get(this.getBiomeWithCaching(pos));
    }

    private int sampleNeighbors(final @NonNull BlockPosition pos, final int radius, final @NonNull BiFunction<BiomeBase, BlockPosition, Integer> colorSampler) {
        int rgb, r = 0, g = 0, b = 0, count = 0;
        for (int x = pos.getX() - radius; x < pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z < pos.getZ() + radius; z++) {
                sharedBlockPos.setValues(x, pos.getY(), z);
                final BiomeBase biome = this.getBiomeWithCaching(sharedBlockPos);
                rgb = colorSampler.apply(biome, this.sharedBlockPos);
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

    private BiomeBase getBiomeWithCaching(final @NonNull BlockPosition pos) {
        long xz = (long) pos.getX() << 32 | pos.getZ() & 0xffffffffL;
        BiomeBase biome = blockPosBiomeCache.getIfPresent(xz);
        if (biome == null) {
            biome = world.getBiome(pos);
            blockPosBiomeCache.put(xz, biome);
        }
        return biome;
    }

    private IRegistry<BiomeBase> getBiomeRegistry() {
        return world.r().b(IRegistry.ay);
    }

    private static int modifiedGrassColor(final @NonNull BiomeBase biome, final @NonNull BlockPosition pos, final int color) {
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

        @SuppressWarnings("unchecked")
        private static @NonNull Optional<Integer> grassColor(final @NonNull BiomeBase biome) {
            try {
                return (Optional<Integer>) grass_color.get(biomeEffects(biome));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Could not find grass color", e);
            }
        }

        @SuppressWarnings("unchecked")
        private static Optional<Integer> foliageColor(final @NonNull BiomeBase biome) {
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

        private static BiomeFog biomeEffects(BiomeBase biome) {
            return biome.l();
        }
    }
}
