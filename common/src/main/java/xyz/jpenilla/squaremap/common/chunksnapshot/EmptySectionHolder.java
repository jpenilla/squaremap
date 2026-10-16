package xyz.jpenilla.squaremap.common.chunksnapshot;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class EmptySectionHolder {
    private static @MonotonicNonNull SectionData<BlockState> EMPTY_SECTION_BLOCK_STATES;
    private static @MonotonicNonNull SectionData<Holder<Biome>> EMPTY_SECTION_BIOMES;

    private EmptySectionHolder() {
    }

    public static void init(final PalettedContainerFactory palettedContainerFactory) {
        if (EMPTY_SECTION_BLOCK_STATES == null) {
            EMPTY_SECTION_BLOCK_STATES = SectionData.Packed.single(Blocks.AIR.defaultBlockState());
        }
        // Always replace, biomes can change between integrated server runs
        EMPTY_SECTION_BIOMES = SectionData.Packed.single(palettedContainerFactory.defaultBiome());
    }

    static SectionData<BlockState> getEmptySectionBlockStates() {
        if (EMPTY_SECTION_BLOCK_STATES == null) {
            throw new IllegalStateException("EmptySectionHolder not initialized");
        }
        return EMPTY_SECTION_BLOCK_STATES;
    }

    static SectionData<Holder<Biome>> getEmptySectionBiomes() {
        if (EMPTY_SECTION_BIOMES == null) {
            throw new IllegalStateException("EmptySectionHolder not initialized");
        }
        return EMPTY_SECTION_BIOMES;
    }
}
