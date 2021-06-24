package net.pl3x.map.plugin.configuration;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.pl3x.map.plugin.data.BiomeColors;
import net.pl3x.map.plugin.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public class WorldAdvanced extends AbstractWorldConfig {
    private static final Map<UUID, WorldAdvanced> configs = new HashMap<>();

    public static void reload() {
        configs.clear();
        Bukkit.getWorlds().forEach(WorldAdvanced::get);
    }

    public static WorldAdvanced get(final @NonNull World world) {
        WorldAdvanced config = configs.get(world.getUID());
        if (config == null) {
            config = new WorldAdvanced(world, Advanced.config);
            configs.put(world.getUID(), config);
        }
        return config;
    }

    WorldAdvanced(World world, AbstractConfig parent) {
        super(world, parent);
        init();
    }

    void init() {
        this.config.readConfig(WorldAdvanced.class, this);
    }

    public final List<Block> invisibleBlocks = new ArrayList<>();

    private void invisibleBlocks() {
        invisibleBlocks.clear();
        getList("invisible-blocks", List.of(
                "minecraft:tall_grass",
                "minecraft:fern",
                "minecraft:grass",
                "minecraft:large_fern"
        )).forEach(block -> invisibleBlocks.add(Registry.BLOCK.get(new ResourceLocation(block.toString()))));
    }

    public final List<Block> iterateUpBaseBlocks = new ArrayList<>();

    private void iterateUpBaseBlocks() {
        iterateUpBaseBlocks.clear();
        getList("iterate-up-base-blocks", List.of(
                "minecraft:netherrack",
                "minecraft:glowstone",
                "minecraft:soul_sand",
                "minecraft:soul_soil",
                "minecraft:gravel",
                "minecraft:warped_nylium",
                "minecraft:crimson_nylium",
                "minecraft:nether_gold_ore",
                "minecraft:ancient_debris",
                "minecraft:nether_quartz_ore",
                "minecraft:magma_block",
                "minecraft:basalt"
        )).forEach(block -> iterateUpBaseBlocks.add(Registry.BLOCK.get(new ResourceLocation(block.toString()))));
    }

    public final Map<Biome, Integer> COLOR_OVERRIDES_BIOME_FOLIAGE = new HashMap<>();

    private void colorOverrideBiomeFoliageSettings() {
        final Registry<Biome> registry = BiomeColors.getBiomeRegistry(world);
        COLOR_OVERRIDES_BIOME_FOLIAGE.clear();
        getMap("color-overrides.biomes.foliage", Map.ofEntries(
                Map.entry("minecraft:dark_forest", "#1c7b07"),
                Map.entry("minecraft:dark_forest_hills", "#1c7b07"),
                Map.entry("minecraft:jungle", "#1f8907"),
                Map.entry("minecraft:jungle_edge", "#1f8907"),
                Map.entry("minecraft:jungle_hills", "#1f8907"),
                Map.entry("minecraft:bamboo_jungle", "#1f8907"),
                Map.entry("minecraft:bamboo_jungle_hills", "#1f8907")
        )).forEach((key, color) -> {
            final Biome biome = registry.get(new ResourceLocation(key));
            if (biome != null) {
                COLOR_OVERRIDES_BIOME_FOLIAGE.put(biome, Colors.parseHex(color));
            }
        });
    }

    public final Map<Biome, Integer> COLOR_OVERRIDES_BIOME_GRASS = new HashMap<>();

    private void colorOverrideBiomeGrassSettings() {
        final Registry<Biome> registry = BiomeColors.getBiomeRegistry(world);
        COLOR_OVERRIDES_BIOME_GRASS.clear();
        getMap("color-overrides.biomes.grass", Map.<String, String>ofEntries(
        )).forEach((key, color) -> {
            final Biome biome = registry.get(new ResourceLocation(key));
            if (biome != null) {
                COLOR_OVERRIDES_BIOME_GRASS.put(biome, Colors.parseHex(color));
            }
        });
    }

    public final Map<Biome, Integer> COLOR_OVERRIDES_BIOME_WATER = new HashMap<>();

    private void colorOverrideBiomeWaterSettings() {
        final Registry<Biome> registry = BiomeColors.getBiomeRegistry(world);
        COLOR_OVERRIDES_BIOME_WATER.clear();
        getMap("color-overrides.biomes.water", Map.<String, String>ofEntries(
        )).forEach((key, color) -> {
            final Biome biome = registry.get(new ResourceLocation(key));
            if (biome != null) {
                COLOR_OVERRIDES_BIOME_WATER.put(biome, Colors.parseHex(color));
            }
        });
    }

    public final Map<Block, Integer> COLOR_OVERRIDES_BLOCKS = new HashMap<>();

    private void colorOverrideBlocksSettings() {
        COLOR_OVERRIDES_BLOCKS.clear();
        getMap("color-overrides.blocks", Map.ofEntries(
                Map.entry("minecraft:mycelium", "#6F6265"),
                Map.entry("minecraft:terracotta", "#9E6246"),
                Map.entry("minecraft:dandelion", "#FFEC4F"),
                Map.entry("minecraft:poppy", "#ED302C"),
                Map.entry("minecraft:blue_orchid", "#2ABFFD"),
                Map.entry("minecraft:allium", "#B878ED"),
                Map.entry("minecraft:azure_bluet", "#F7F7F7"),
                Map.entry("minecraft:red_tulip", "#9B221A"),
                Map.entry("minecraft:orange_tulip", "#BD6A22"),
                Map.entry("minecraft:pink_tulip", "#EBC5FD"),
                Map.entry("minecraft:white_tulip", "#D6E8E8"),
                Map.entry("minecraft:oxeye_daisy", "#D6E8E8"),
                Map.entry("minecraft:cornflower", "#466AEB"),
                Map.entry("minecraft:lily_of_the_valley", "#FFFFFF"),
                Map.entry("minecraft:wither_rose", "#211A16"),
                Map.entry("minecraft:sunflower", "#FFEC4F"),
                Map.entry("minecraft:lilac", "#B66BB2"),
                Map.entry("minecraft:rose_bush", "#9B221A"),
                Map.entry("minecraft:peony", "#EBC5FD"),
                Map.entry("minecraft:lily_pad", "#208030"),
                Map.entry("minecraft:attached_melon_stem", "#E0C71C"),
                Map.entry("minecraft:attached_pumpkin_stem", "#E0C71C"),
                Map.entry("minecraft:spruce_leaves", "#619961"),
                Map.entry("minecraft:birch_leaves", "#80A755"),
                Map.entry("minecraft:lava", "#EA5C0F"),
                Map.entry("minecraft:glass", "#FFFFFF")
        )).forEach((key, color) -> {
            final Block block = Registry.BLOCK.get(new ResourceLocation(key));
            if (block != Blocks.AIR) {
                COLOR_OVERRIDES_BLOCKS.put(block, Colors.parseHex(color));
            }
        });
    }

}
