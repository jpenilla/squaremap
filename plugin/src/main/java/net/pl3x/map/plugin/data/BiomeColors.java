package net.pl3x.map.plugin.data;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Material;
import net.pl3x.map.plugin.util.Colors;
import net.pl3x.map.plugin.util.FileUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

public final class BiomeColors {
    private static final Set<Block> grassColorBlocks = Set.of(
            Blocks.GRASS_BLOCK,
            Blocks.GRASS,
            Blocks.TALL_GRASS,
            Blocks.FERN,
            Blocks.LARGE_FERN,
            Blocks.POTTED_FERN,
            Blocks.SUGAR_CANE
    );

    private static final Set<Block> foliageColorBlocks = Set.of(
            Blocks.VINE,
            Blocks.OAK_LEAVES,
            Blocks.JUNGLE_LEAVES,
            Blocks.ACACIA_LEAVES,
            Blocks.DARK_OAK_LEAVES
    );

    private static final Set<Block> waterColorBlocks = Set.of(
            Blocks.WATER,
            Blocks.BUBBLE_COLUMN,
            Blocks.WATER_CAULDRON
    );

    private static final Set<Material> waterColorMaterials = Set.of(
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

    private final Cache<Long, Biome> blockPosBiomeCache = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.SECONDS).maximumSize(100000L).build();

    private final MapWorld world;

    private final Reference2IntMap<Biome> grassColors = new Reference2IntOpenHashMap<>();
    private final Reference2IntMap<Biome> foliageColors = new Reference2IntOpenHashMap<>();
    private final Reference2IntMap<Biome> waterColors = new Reference2IntOpenHashMap<>();

    private final BlockPos.MutableBlockPos sharedBlockPos = new BlockPos.MutableBlockPos();

    public BiomeColors(final @NonNull MapWorld world) {
        this.world = world;

        final Registry<Biome> biomeRegistry = biomeRegistry(world.nms());
        for (final Biome biome : biomeRegistry) {
            float temperature = Mth.clamp(biome.getBaseTemperature(), 0.0F, 1.0F);
            float humidity = Mth.clamp(biome.getDownfall(), 0.0F, 1.0F);
            this.grassColors.put(biome, BiomeSpecialEffectsHelper.grassColor(biome)
                    .orElse(getDefaultGrassColor(temperature, humidity)).intValue());
            this.foliageColors.put(biome, BiomeSpecialEffectsHelper.foliageColor(biome)
                    .orElse(Colors.mix(Colors.leavesMapColor(), getDefaultFoliageColor(temperature, humidity), 0.85f)).intValue());
            this.waterColors.put(biome, BiomeSpecialEffectsHelper.waterColor(biome));
        }

        world.advanced().COLOR_OVERRIDES_BIOME_FOLIAGE.forEach((key, value) -> this.foliageColors.put(key, value.intValue()));
        world.advanced().COLOR_OVERRIDES_BIOME_GRASS.forEach((key, value) -> this.grassColors.put(key, value.intValue()));
        world.advanced().COLOR_OVERRIDES_BIOME_WATER.forEach((key, value) -> this.waterColors.put(key, value.intValue()));
    }

    public int modifyColorFromBiome(int color, final @NonNull LevelChunk chunk, final @NonNull BlockPos pos) {
        final BlockState data = chunk.getBlockState(pos);
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
        final int[] map = new int[256 * 256];
        for (int x = 0; x < 256; ++x) {
            for (int y = 0; y < 256; ++y) {
                final int color = image.getRGB(x, y);
                final int r = color >> 16 & 0xFF;
                final int g = color >> 8 & 0xFF;
                final int b = color & 0xFF;
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

    private int grass(final @NonNull BlockPos pos) {
        if (this.world.config().MAP_BIOMES_BLEND > 0) {
            return this.sampleNeighbors(pos, this.world.config().MAP_BIOMES_BLEND, this::grassColorSampler);
        }
        return this.grassColorSampler(this.getBiomeWithCaching(pos), pos);
    }

    private int grassColorSampler(final @NonNull Biome biome, final @NonNull BlockPos pos) {
        return modifiedGrassColor(biome, pos, this.grassColors.getInt(biome));
    }

    private int foliage(final @NonNull BlockPos pos) {
        if (this.world.config().MAP_BIOMES_BLEND > 0) {
            return this.sampleNeighbors(pos, this.world.config().MAP_BIOMES_BLEND, (biome, b) -> this.foliageColors.getInt(biome));
        }
        return this.foliageColors.getInt(this.getBiomeWithCaching(pos));
    }

    private int water(final @NonNull BlockPos pos) {
        if (this.world.config().MAP_BIOMES_BLEND > 0) {
            return this.sampleNeighbors(pos, this.world.config().MAP_BIOMES_BLEND, (biome, b) -> this.waterColors.getInt(biome));
        }
        return this.waterColors.getInt(this.getBiomeWithCaching(pos));
    }

    interface ColorSampler {
        int sample(Biome biome, BlockPos pos);
    }

    private int sampleNeighbors(final @NonNull BlockPos pos, final int radius, final @NonNull ColorSampler colorSampler) {
        int rgb, r = 0, g = 0, b = 0, count = 0;
        // Sampling in the y direction as well would improve output, however would complicate caching (low priority, PRs accepted)
        for (int x = pos.getX() - radius; x < pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z < pos.getZ() + radius; z++) {
                this.sharedBlockPos.set(x, pos.getY(), z);
                final Biome biome = this.getBiomeWithCaching(this.sharedBlockPos);
                rgb = colorSampler.sample(biome, this.sharedBlockPos);
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

    private Biome getBiomeWithCaching(final @NonNull BlockPos pos) {
        long xz = (long) pos.getX() << 32 | pos.getZ() & 0xffffffffL;
        Biome biome = this.blockPosBiomeCache.getIfPresent(xz);
        if (biome == null) {
            biome = this.world.nms().getBiome(pos);
            this.blockPosBiomeCache.put(xz, biome);
        }
        return biome;
    }

    public static Registry<Biome> biomeRegistry(ServerLevel world) {
        return world.registryAccess().ownedRegistryOrThrow(Registry.BIOME_REGISTRY);
    }

    private static int modifiedGrassColor(final @NonNull Biome biome, final @NonNull BlockPos pos, final int color) {
        BiomeSpecialEffects.GrassColorModifier modifier = BiomeSpecialEffectsHelper.grassColorModifier(biome);
        switch (modifier) {
            case NONE:
                return color;
            case SWAMP:
                // swamps have 2 grass colors, depends on sample from noise generator
                double sample = Biome.BIOME_INFO_NOISE.getValue(pos.getX() * 0.0225, pos.getZ() * 0.0225, false);
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
    private static final class BiomeSpecialEffectsHelper {
        private BiomeSpecialEffectsHelper() {
        }

        private static final BiomeSpecialEffectsProxy BIOME_SPECIAL_EFFECTS;

        static {
            final ReflectionRemapper reflectionRemapper = ReflectionRemapper.forReobfMappingsInPaperJar();
            final ReflectionProxyFactory factory = ReflectionProxyFactory.create(reflectionRemapper, BiomeSpecialEffectsHelper.class.getClassLoader());

            BIOME_SPECIAL_EFFECTS = factory.reflectionProxy(BiomeSpecialEffectsProxy.class);
        }

        @Proxies(BiomeSpecialEffects.class)
        interface BiomeSpecialEffectsProxy {
            Optional<Integer> grassColorOverride(BiomeSpecialEffects effects);

            Optional<Integer> foliageColorOverride(BiomeSpecialEffects effects);

            BiomeSpecialEffects.GrassColorModifier grassColorModifier(BiomeSpecialEffects effects);

            int waterColor(BiomeSpecialEffects effects);
        }

        private static @NonNull Optional<Integer> grassColor(final @NonNull Biome biome) {
            return BIOME_SPECIAL_EFFECTS.grassColorOverride(biome.getSpecialEffects());
        }

        private static Optional<Integer> foliageColor(final @NonNull Biome biome) {
            return BIOME_SPECIAL_EFFECTS.foliageColorOverride(biome.getSpecialEffects());
        }

        private static BiomeSpecialEffects.@NonNull GrassColorModifier grassColorModifier(final @NonNull Biome biome) {
            return BIOME_SPECIAL_EFFECTS.grassColorModifier(biome.getSpecialEffects());
        }

        private static int waterColor(final @NonNull Biome biome) {
            return BIOME_SPECIAL_EFFECTS.waterColor(biome.getSpecialEffects());
        }
    }
}
