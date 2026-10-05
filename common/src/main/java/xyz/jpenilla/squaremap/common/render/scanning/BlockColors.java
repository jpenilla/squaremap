package xyz.jpenilla.squaremap.common.render.scanning;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.WorldAdvanced;
import xyz.jpenilla.squaremap.common.util.Colors;

@DefaultQualifier(NonNull.class)
public final class BlockColors {
    private final Reference2IntMap<Block> staticColorMap;
    private final Reference2ObjectMap<Block, DynamicColorGetter> dynamicColorMap;

    public BlockColors(final WorldAdvanced advanced) {
        final Reference2IntMap<Block> staticColors = new Reference2IntOpenHashMap<>(advanced.COLOR_OVERRIDES_BLOCKS);
        staticColors.defaultReturnValue(-1);
        this.staticColorMap = Reference2IntMaps.unmodifiable(staticColors);

        this.dynamicColorMap = this.loadDynamicColors();
    }

    private Reference2ObjectMap<Block, DynamicColorGetter> loadDynamicColors() {
        final Map<Block, DynamicColorGetter> map = new HashMap<>();

        map.put(Blocks.MELON_STEM, BlockColors::melonAndPumpkinStem);
        map.put(Blocks.PUMPKIN_STEM, BlockColors::melonAndPumpkinStem);
        map.put(Blocks.WHEAT, BlockColors::wheat);

        return Reference2ObjectMaps.unmodifiable(new Reference2ObjectOpenHashMap<>(map));
    }

    /**
     * Returns a configured or state-dependent block color, falling back to the vanilla map color.
     *
     * @param state the block state
     * @return the map color
     */
    public int color(final BlockState state) {
        final Block block = state.getBlock();

        final int staticColor = this.staticColorMap.getInt(block);
        if (staticColor != -1) {
            return staticColor;
        }

        final @Nullable DynamicColorGetter func = this.dynamicColorMap.get(block);
        if (func != null) {
            final int dynamicColor = func.color(state);
            if (dynamicColor != -1) {
                return dynamicColor;
            }
        }

        // getMapColor params are never used by vanilla - check on update
        // They are however used by certain mods like framed blocks, so we pass dummy values to avoid errors.
        // Proper framed blocks compatibility would require including block entities in the snapshot and passing the real position.
        // We would probably want to whitelist block entity types for performance and safety reasons.
        // Generally, we can't support 100% of possible modded uses of these parameters because of our off-main-thread chunk snapshot use.
        return Colors.rgb(state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    private static int melonAndPumpkinStem(final BlockState state) {
        int age = state.getValue(StemBlock.AGE);
        int k = age * 32;
        int l = 255 - age * 8;
        int m = age * 4;
        return k << 16 | l << 8 | m;
    }

    private static int wheat(final BlockState state) {
        float factor = (state.getValue(CropBlock.AGE) + 1) / 8F;
        return Colors.mix(Colors.plantMapColor(), 0xDCBB65, factor);
    }

    @FunctionalInterface
    private interface DynamicColorGetter {
        int color(BlockState state);
    }
}
