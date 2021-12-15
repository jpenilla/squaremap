package xyz.jpenilla.squaremap.plugin.configuration;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.plugin.data.BiomeColors;
import xyz.jpenilla.squaremap.plugin.util.Colors;

@SuppressWarnings("unused")
public final class WorldAdvanced extends AbstractWorldConfig {
    private static final Map<UUID, WorldAdvanced> CONFIG_MAP = new HashMap<>();

    public static void reload() {
        reload(WorldAdvanced.class, CONFIG_MAP, WorldAdvanced::get);
    }

    public static WorldAdvanced get(final @NonNull World world) {
        return CONFIG_MAP.computeIfAbsent(world.getUID(), $ -> new WorldAdvanced(world, Advanced.config));
    }

    private WorldAdvanced(final World world, final AbstractConfig parent) {
        super(world, parent);
        this.init();
    }

    void init() {
        this.config.readConfig(WorldAdvanced.class, this);
    }

    public final Set<Block> invisibleBlocks = new HashSet<>();

    private void invisibleBlocks() {
        this.invisibleBlocks.clear();
        getList("invisible-blocks", List.of(
            "minecraft:tall_grass",
            "minecraft:fern",
            "minecraft:grass",
            "minecraft:large_fern"
        )).forEach(block -> this.invisibleBlocks.add(Registry.BLOCK.get(new ResourceLocation(block.toString()))));
    }

    public final Set<Block> iterateUpBaseBlocks = new HashSet<>();

    private void iterateUpBaseBlocks() {
        this.iterateUpBaseBlocks.clear();
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
        )).forEach(block -> this.iterateUpBaseBlocks.add(Registry.BLOCK.get(new ResourceLocation(block.toString()))));
    }

    public final Reference2IntMap<Biome> COLOR_OVERRIDES_BIOME_FOLIAGE = new Reference2IntOpenHashMap<>();

    private void colorOverrideBiomeFoliageSettings() {
        final Registry<Biome> registry = BiomeColors.biomeRegistry(this.world);
        this.COLOR_OVERRIDES_BIOME_FOLIAGE.clear();
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
                this.COLOR_OVERRIDES_BIOME_FOLIAGE.put(biome, Colors.parseHex(color));
            }
        });
    }

    public final Reference2IntMap<Biome> COLOR_OVERRIDES_BIOME_GRASS = new Reference2IntOpenHashMap<>();

    private void colorOverrideBiomeGrassSettings() {
        final Registry<Biome> registry = BiomeColors.biomeRegistry(this.world);
        this.COLOR_OVERRIDES_BIOME_GRASS.clear();
        getMap("color-overrides.biomes.grass", Map.<String, String>ofEntries()).forEach((key, color) -> {
            final Biome biome = registry.get(new ResourceLocation(key));
            if (biome != null) {
                this.COLOR_OVERRIDES_BIOME_GRASS.put(biome, Colors.parseHex(color));
            }
        });
    }

    public final Reference2IntMap<Biome> COLOR_OVERRIDES_BIOME_WATER = new Reference2IntOpenHashMap<>();

    private void colorOverrideBiomeWaterSettings() {
        final Registry<Biome> registry = BiomeColors.biomeRegistry(this.world);
        this.COLOR_OVERRIDES_BIOME_WATER.clear();
        getMap("color-overrides.biomes.water", Map.<String, String>ofEntries()).forEach((key, color) -> {
            final Biome biome = registry.get(new ResourceLocation(key));
            if (biome != null) {
                this.COLOR_OVERRIDES_BIOME_WATER.put(biome, Colors.parseHex(color));
            }
        });
    }

    public final Reference2IntMap<Block> COLOR_OVERRIDES_BLOCKS = new Reference2IntOpenHashMap<>();

    private void colorOverrideBlocksSettings() {
        this.COLOR_OVERRIDES_BLOCKS.clear();
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
                this.COLOR_OVERRIDES_BLOCKS.put(block, Colors.parseHex(color));
            }
        });
    }

}
