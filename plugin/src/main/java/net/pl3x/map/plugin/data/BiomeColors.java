package net.pl3x.map.plugin.data;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
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
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.pl3x.map.plugin.util.Colors;
import net.pl3x.map.plugin.util.FileUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.squaremap.plugin.util.ChunkSnapshot;

@DefaultQualifier(NonNull.class)
public final class BiomeColors {
    private static final int CHUNK_SNAPSHOT_CACHE_SIZE = 128;

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

    private final Cache<Long, Biome> blockPosBiomeCache = CacheBuilder.newBuilder()
        .expireAfterAccess(10L, TimeUnit.SECONDS)
        .maximumSize(100000L)
        .build();
    private final ChunkSnapshotCache chunkSnapshotCache;

    private final MapWorld world;

    private final Reference2IntMap<Biome> grassColors = new Reference2IntOpenHashMap<>();
    private final Reference2IntMap<Biome> foliageColors = new Reference2IntOpenHashMap<>();
    private final Reference2IntMap<Biome> waterColors = new Reference2IntOpenHashMap<>();

    private final BlockPos.MutableBlockPos sharedBlockPos = new BlockPos.MutableBlockPos();

    public BiomeColors(final MapWorld world) {
        this.world = world;
        this.chunkSnapshotCache = ChunkSnapshotCache.sized(this.world.serverLevel(), CHUNK_SNAPSHOT_CACHE_SIZE);

        final Registry<Biome> biomeRegistry = biomeRegistry(world.serverLevel());
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

    public int modifyColorFromBiome(int color, final ChunkSnapshot chunk, final BlockPos pos) {
        this.chunkSnapshotCache.put(chunk.pos().toLong(), chunk);

        final BlockState data = chunk.getBlockState(pos);
        final Material mat = data.getMaterial();
        final Block block = data.getBlock();

        if (grassColorBlocks.contains(block)) {
            color = this.grass(pos);
        } else if (foliageColorBlocks.contains(block)) {
            color = this.foliage(pos);
        } else if (waterColorBlocks.contains(block) || waterColorMaterials.contains(mat)) {
            int modColor = this.water(pos);
            color = Colors.mix(color, modColor, 0.8F);
        }

        return color;
    }

    private static int[] init(final BufferedImage image) {
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

    private int grass(final BlockPos pos) {
        if (this.world.config().MAP_BIOMES_BLEND > 0) {
            return this.sampleNeighbors(pos, this.world.config().MAP_BIOMES_BLEND, this::grassColorSampler);
        }
        return this.grassColorSampler(this.getBiomeWithCaching(pos), pos);
    }

    private int grassColorSampler(final Biome biome, final BlockPos pos) {
        return modifiedGrassColor(biome, pos, this.grassColors.getInt(biome));
    }

    private int foliage(final BlockPos pos) {
        if (this.world.config().MAP_BIOMES_BLEND > 0) {
            return this.sampleNeighbors(pos, this.world.config().MAP_BIOMES_BLEND, (biome, b) -> this.foliageColors.getInt(biome));
        }
        return this.foliageColors.getInt(this.getBiomeWithCaching(pos));
    }

    private int water(final BlockPos pos) {
        if (this.world.config().MAP_BIOMES_BLEND > 0) {
            return this.sampleNeighbors(pos, this.world.config().MAP_BIOMES_BLEND, (biome, b) -> this.waterColors.getInt(biome));
        }
        return this.waterColors.getInt(this.getBiomeWithCaching(pos));
    }

    interface ColorSampler {
        int sample(Biome biome, BlockPos pos);
    }

    private int sampleNeighbors(final BlockPos pos, final int radius, final ColorSampler colorSampler) {
        int rgb;
        int r = 0;
        int g = 0;
        int b = 0;
        int count = 0;
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

    private Biome getBiomeWithCaching(final BlockPos pos) {
        long xz = (long) pos.getX() << 32 | pos.getZ() & 0xffffffffL;
        @Nullable Biome biome = this.blockPosBiomeCache.getIfPresent(xz);
        if (biome == null) {
            biome = this.world.serverLevel().getBiomeManager()
                .withDifferentSource(this::getNoiseBiome)
                .getBiome(pos);
            this.blockPosBiomeCache.put(xz, biome);
        }
        return biome;
    }

    private Biome getNoiseBiome(int quartX, int quartY, int quartZ) {
        final @Nullable ChunkSnapshot chunk = this.chunkSnapshotCache.snapshot(
            new ChunkPos(QuartPos.toSection(quartX), QuartPos.toSection(quartZ))
        );
        final BiomeManager.NoiseBiomeSource noiseBiomeSource;
        if (chunk == null) {
            noiseBiomeSource = this.world.serverLevel();
        } else {
            noiseBiomeSource = chunk;
        }
        return noiseBiomeSource.getNoiseBiome(quartX, quartY, quartZ);
    }

    public static Registry<Biome> biomeRegistry(ServerLevel world) {
        return world.registryAccess().ownedRegistryOrThrow(Registry.BIOME_REGISTRY);
    }

    private static int modifiedGrassColor(final Biome biome, final BlockPos pos, final int color) {
        return switch (BiomeSpecialEffectsHelper.grassColorModifier(biome)) {
            case NONE -> color;
            case SWAMP -> modifiedSwampGrassColor(pos);
            case DARK_FOREST -> (color & 0xFEFEFE) + 2634762 >> 1;
        };
    }

    private static int modifiedSwampGrassColor(final BlockPos pos) {
        // swamps have 2 grass colors, depends on sample from noise generator
        double sample = Biome.BIOME_INFO_NOISE.getValue(pos.getX() * 0.0225, pos.getZ() * 0.0225, false);
        if (sample < -0.1) {
            return 5011004;
        }
        return 6975545;
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
            @FieldGetter("grassColorOverride")
            Optional<Integer> grassColorOverride(BiomeSpecialEffects effects);

            @FieldGetter("foliageColorOverride")
            Optional<Integer> foliageColorOverride(BiomeSpecialEffects effects);

            @FieldGetter("grassColorModifier")
            BiomeSpecialEffects.GrassColorModifier grassColorModifier(BiomeSpecialEffects effects);

            @FieldGetter("waterColor")
            int waterColor(BiomeSpecialEffects effects);
        }

        private static Optional<Integer> grassColor(final Biome biome) {
            return BIOME_SPECIAL_EFFECTS.grassColorOverride(biome.getSpecialEffects());
        }

        private static Optional<Integer> foliageColor(final Biome biome) {
            return BIOME_SPECIAL_EFFECTS.foliageColorOverride(biome.getSpecialEffects());
        }

        private static BiomeSpecialEffects.GrassColorModifier grassColorModifier(final Biome biome) {
            return BIOME_SPECIAL_EFFECTS.grassColorModifier(biome.getSpecialEffects());
        }

        private static int waterColor(final Biome biome) {
            return BIOME_SPECIAL_EFFECTS.waterColor(biome.getSpecialEffects());
        }
    }

    private record ChunkSnapshotCache(
        ServerLevel level,
        int size,
        Long2ObjectLinkedOpenHashMap<ChunkSnapshot> cache
    ) {
        public void put(long pos, ChunkSnapshot snapshot) {
            if (this.cache.size() >= this.size()) {
                this.cache.removeLast();
            }
            this.cache.putAndMoveToFirst(pos, snapshot);
        }

        public @Nullable ChunkSnapshot snapshot(final ChunkPos chunkPos) {
            final @Nullable ChunkSnapshot cached = this.cache.getAndMoveToFirst(chunkPos.toLong());
            if (cached != null) {
                return cached;
            }

            @Nullable final ChunkSnapshot chunk = ChunkSnapshot.asyncSnapshot(this.level(), chunkPos.x, chunkPos.z, true)
                // todo respect cancellation
                .join();
            if (chunk == null) {
                return null;
            }
            if (this.cache.size() >= this.size()) {
                this.cache.removeLast();
            }
            this.cache.putAndMoveToFirst(chunkPos.toLong(), chunk);
            return chunk;
        }

        public static ChunkSnapshotCache sized(final ServerLevel level, final int size) {
            final Long2ObjectLinkedOpenHashMap<ChunkSnapshot> map = new Long2ObjectLinkedOpenHashMap<>(size);
            return new ChunkSnapshotCache(level, size, map);
        }
    }
}
