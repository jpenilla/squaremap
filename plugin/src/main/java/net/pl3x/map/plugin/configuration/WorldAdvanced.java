package net.pl3x.map.plugin.configuration;

import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.MinecraftKey;
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

    public List<Block> invisibleBlocks = new ArrayList<>();

    private void invisibleBlocks() {
        invisibleBlocks.clear();
        getList("invisible-blocks", List.of(
                "minecraft:tall_grass",
                "minecraft:fern",
                "minecraft:grass",
                "minecraft:large_fern"
        )).forEach(block -> invisibleBlocks.add(IRegistry.BLOCK.get(new MinecraftKey(block.toString()))));
    }

    public List<Block> iterateUpBaseBlocks = new ArrayList<>();

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
        )).forEach(block -> iterateUpBaseBlocks.add(IRegistry.BLOCK.get(new MinecraftKey(block.toString()))));
    }

}
