package xyz.jpenilla.squaremap.common.util.chunksnapshot;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class EmptySectionHolder {
    static @MonotonicNonNull PalettedContainer<BlockState> EMPTY_SECTION_BLOCK_STATES;

    private EmptySectionHolder() {
    }

    public static void init(final PalettedContainerFactory palettedContainerFactory) {
        if (EMPTY_SECTION_BLOCK_STATES == null) {
            EMPTY_SECTION_BLOCK_STATES = new PalettedContainer<>(
                Blocks.AIR.defaultBlockState(),
                palettedContainerFactory.blockStatesStrategy()
            );
        }
    }

    static PalettedContainer<BlockState> getEmptySectionBlockStates() {
        if (EMPTY_SECTION_BLOCK_STATES == null) {
            throw new IllegalStateException("EmptySectionHolder not initialized");
        }
        return EMPTY_SECTION_BLOCK_STATES;
    }
}
