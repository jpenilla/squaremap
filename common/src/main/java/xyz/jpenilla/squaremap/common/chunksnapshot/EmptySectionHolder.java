package xyz.jpenilla.squaremap.common.chunksnapshot;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class EmptySectionHolder {
    private static @MonotonicNonNull PalettedContainer<BlockState> EMPTY_SECTION_BLOCK_STATES;
    private static @MonotonicNonNull PalettedContainer<Holder<Biome>> EMPTY_SECTION_BIOMES;

    private EmptySectionHolder() {
    }

    public static void init(final PalettedContainerFactory palettedContainerFactory) {
        if (EMPTY_SECTION_BLOCK_STATES == null) {
            EMPTY_SECTION_BLOCK_STATES = new PalettedContainer<>(
                Blocks.AIR.defaultBlockState(),
                palettedContainerFactory.blockStatesStrategy()
            );
        }
        if (EMPTY_SECTION_BIOMES == null) {
            EMPTY_SECTION_BIOMES = palettedContainerFactory.createForBiomes();
        }
    }

    static PalettedContainer<BlockState> getEmptySectionBlockStates() {
        if (EMPTY_SECTION_BLOCK_STATES == null) {
            throw new IllegalStateException("EmptySectionHolder not initialized");
        }
        return EMPTY_SECTION_BLOCK_STATES;
    }

    static PalettedContainer<Holder<Biome>> getEmptySectionBiomes() {
        if (EMPTY_SECTION_BIOMES == null) {
            throw new IllegalStateException("EmptySectionHolder not initialized");
        }
        return EMPTY_SECTION_BIOMES;
    }
}
