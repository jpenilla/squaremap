package net.pl3x.map.plugin.data;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCrops;
import net.minecraft.world.level.block.BlockStem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.pl3x.map.plugin.util.Colors;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.function.Function;

public final class BlockColors {
    private final Map<Block, Integer> staticColorMap;
    private final Map<Block, Function<IBlockData, Integer>> dynamicColorMap;

    public BlockColors(MapWorld world) {
        this.staticColorMap = ImmutableMap.copyOf(world.advanced().COLOR_OVERRIDES_BLOCKS);
        this.dynamicColorMap = this.loadDynamicColors();
    }

    private @NonNull Map<Block, Function<IBlockData, Integer>> loadDynamicColors() {
        final ImmutableMap.Builder<Block, Function<IBlockData, Integer>> dynamicColorBuilder = ImmutableMap.builder();

        dynamicColorBuilder.put(Blocks.dW, BlockColors::melonAndPumpkinStem); // TODO MELON_STEM
        dynamicColorBuilder.put(Blocks.dV, BlockColors::melonAndPumpkinStem); // TODO PUMPKIN_STEM
        dynamicColorBuilder.put(Blocks.cd, BlockColors::wheat); // TODO WHEAT

        return dynamicColorBuilder.build();
    }

    /**
     * Get a special color for a IBlockData, it it exists. Will return -1 if there
     * is no special color for the provided IBlockData.
     *
     * @param state IBlockData to test
     * @return special color, or -1
     */
    public int getColor(final @NonNull IBlockData state) {
        final Block block = state.getBlock();

        final Integer staticColor = this.staticColorMap.get(block);
        if (staticColor != null) {
            return staticColor;
        }

        final Function<IBlockData, Integer> func = this.dynamicColorMap.get(block);
        if (func != null) {
            return func.apply(state);
        }

        return -1;
    }

    private static int melonAndPumpkinStem(final @NonNull IBlockData state) {
        int age = state.get(BlockStem.b); // TODO StemBlock.AGE
        int k = age * 32;
        int l = 255 - age * 8;
        int m = age * 4;
        return k << 16 | l << 8 | m;
    }

    private static int wheat(final @NonNull IBlockData state) {
        float factor = (state.get(BlockCrops.d) + 1) / 8F; // TODO CropBlock.AGE
        return Colors.mix(Colors.plantsMapColor(), 0xDCBB65, factor);
    }

}
