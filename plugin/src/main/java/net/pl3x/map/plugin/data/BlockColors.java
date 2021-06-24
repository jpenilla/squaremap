package net.pl3x.map.plugin.data;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.pl3x.map.plugin.util.Colors;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class BlockColors {
    private final Map<Block, Integer> staticColorMap;
    private final Map<Block, Function<BlockState, Integer>> dynamicColorMap;

    public BlockColors(MapWorld world) {
        this.staticColorMap = ImmutableMap.copyOf(world.advanced().COLOR_OVERRIDES_BLOCKS);
        this.dynamicColorMap = this.loadDynamicColors();
    }

    private @NonNull Map<Block, Function<BlockState, Integer>> loadDynamicColors() {
        final ImmutableMap.Builder<Block, Function<BlockState, Integer>> dynamicColorBuilder = ImmutableMap.builder();

        dynamicColorBuilder.put(Blocks.MELON_STEM, BlockColors::melonAndPumpkinStem);
        dynamicColorBuilder.put(Blocks.PUMPKIN_STEM, BlockColors::melonAndPumpkinStem);
        dynamicColorBuilder.put(Blocks.WHEAT, BlockColors::wheat);

        return dynamicColorBuilder.build();
    }

    /**
     * Get a special color for a IBlockData, it it exists. Will return -1 if there
     * is no special color for the provided IBlockData.
     *
     * @param state IBlockData to test
     * @return special color, or -1
     */
    public int getColor(final @NonNull BlockState state) {
        final Block block = state.getBlock();

        final Integer staticColor = this.staticColorMap.get(block);
        if (staticColor != null) {
            return staticColor;
        }

        final Function<BlockState, Integer> func = this.dynamicColorMap.get(block);
        if (func != null) {
            return func.apply(state);
        }

        return -1;
    }

    private static int melonAndPumpkinStem(final @NonNull BlockState state) {
        int age = state.getValue(StemBlock.AGE);
        int k = age * 32;
        int l = 255 - age * 8;
        int m = age * 4;
        return k << 16 | l << 8 | m;
    }

    private static int wheat(final @NonNull BlockState state) {
        float factor = (state.getValue(CropBlock.AGE) + 1) / 8F;
        return Colors.mix(Colors.plantsMapColor(), 0xDCBB65, factor);
    }

}
