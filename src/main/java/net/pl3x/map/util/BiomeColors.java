package net.pl3x.map.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import net.minecraft.server.v1_16_R3.BiomeBase;
import net.minecraft.server.v1_16_R3.BiomeFog;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.BlockStem;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.IRegistryWritable;
import net.minecraft.server.v1_16_R3.Material;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.World;
import net.pl3x.map.configuration.WorldConfig;
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

public final class BiomeColors {
    private final Cache<Long, BiomeBase> blockPosBiomeCache = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.SECONDS).maximumSize(100000L).build();

    private final World world;
    private final WorldConfig worldConfig;

    private final Map<BiomeBase, Integer> grassColors = new HashMap<>();
    private final Map<BiomeBase, Integer> foliageColors = new HashMap<>();
    private final Map<BiomeBase, Integer> waterColors = new HashMap<>();

    private final BlockPosition.MutableBlockPosition sharedBlockPos = new BlockPosition.MutableBlockPosition();

    private BiomeColors(World world) {
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

    public static @NonNull BiomeColors forWorld(final @NonNull World world) {
        return new BiomeColors(world);
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

    private int grass(final @NonNull BlockPosition pos) {
        if (this.worldConfig.MAP_BIOMES_BLEND > 0) {
            return smooth(pos, this.worldConfig.MAP_BIOMES_BLEND, this.grassColors);
        }
        return this.grassColors.get(this.getBiomeWithCaching(pos));
    }

    private int foliage(final @NonNull BlockPosition pos) {
        if (this.worldConfig.MAP_BIOMES_BLEND > 0) {
            return smooth(pos, this.worldConfig.MAP_BIOMES_BLEND, this.foliageColors);
        }
        return this.foliageColors.get(this.getBiomeWithCaching(pos));
    }

    private int water(final @NonNull BlockPosition pos) {
        if (this.worldConfig.MAP_WATER_BIOMES_BLEND > 0) {
            return smooth(pos, this.worldConfig.MAP_WATER_BIOMES_BLEND, this.waterColors);
        }
        return this.waterColors.get(this.getBiomeWithCaching(pos));
    }

    private int smooth(final @NonNull BlockPosition pos, final int radius, final @NonNull Map<BiomeBase, Integer> colorMap) {
        int rgb, r = 0, g = 0, b = 0, count = 0;
        for (int x = pos.getX() - radius; x < pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z < pos.getZ() + radius; z++) {
                sharedBlockPos.setValues(x, pos.getY(), z);
                final BiomeBase biome = this.getBiomeWithCaching(sharedBlockPos);
                rgb = colorMap.get(biome);
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

    public int modifyColorFromBiome(int color, final @NonNull Chunk chunk, final @NonNull BlockPosition pos) {
        final IBlockData data = chunk.getType(pos);
        final Material mat = data.getMaterial();
        final Block block = data.getBlock();

        if (grassColorBlocks.contains(block)) {

            int modColor = this.grass(pos);
            color = modifiedGrassColor(this.getBiomeWithCaching(pos), pos, modColor);

        } else if (foliageColorBlocks.contains(block)) {

            if (block == Blocks.DARK_OAK_LEAVES) {
                // Dark oak leaves need to be darker than oak, but in the case a custom biome foliage color is used, they should not get darkened
                final int finalColor = color;
                color = BiomeEffectsReflection.foliageColor(this.getBiomeWithCaching(pos)).orElseGet(() -> {
                    int modColor = Colors.mix(finalColor, foliage(pos), 0.75F);
                    return (modColor & 0xFEFEFE) + 2634762 >> 1;
                });
            } else {
                color = Colors.mix(color, foliage(pos), 0.75F);
            }

        } else if (waterColorBlocks.contains(block) || waterColorMaterials.contains(mat)) {

            int modColor = water(pos);
            color = Colors.mix(color, modColor, 0.8F);

        } else if (block == Blocks.SPRUCE_LEAVES) {

            color = Colors.mix(color, 0x619961, 0.5F);

        } else if (block == Blocks.BIRCH_LEAVES) {

            color = Colors.mix(color, 8431445, 0.5F);

        } else if (block == Blocks.LILY_PAD) {
            color = 2129968;
        } else if (block == Blocks.ATTACHED_MELON_STEM || block == Blocks.ATTACHED_PUMPKIN_STEM) {
            color = 14731036;
        } else if (block == Blocks.MELON_STEM || block == Blocks.PUMPKIN_STEM) {
            int j = data.get(BlockStem.AGE);
            int k = j * 32;
            int l = 255 - j * 8;
            int m = j * 4;
            color = k << 16 | l << 8 | m;
        }

        return color;
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
