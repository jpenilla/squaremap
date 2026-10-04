package xyz.jpenilla.squaremap.common.chunksnapshot;

import java.util.function.Predicate;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;

final class HeightmapSnapshot {
    private final BitStorage data;
    private final LevelHeightAccessor heightAccessor;

    HeightmapSnapshot(
        final ChunkAccess chunk,
        final LevelHeightAccessor heightAccessor,
        final Heightmap.Types heightmapType
    ) {
        this(chunk.getOrCreateHeightmapUnprimed(heightmapType).getRawData().clone(), heightAccessor);
    }

    HeightmapSnapshot(
        final long[] data,
        final LevelHeightAccessor heightAccessor
    ) {
        this.data = new SimpleBitStorage(
            Mth.ceillog2(heightAccessor.getHeight() + 1),
            256,
            data
        );
        this.heightAccessor = heightAccessor;
    }

    static HeightmapSnapshot computeWorldSurface(
        final LevelHeightAccessor heightAccessor,
        final PalettedContainer<BlockState>[] sections,
        final boolean[] empty
    ) {
        final SimpleBitStorage data = new SimpleBitStorage(Mth.ceillog2(heightAccessor.getHeight() + 1), 256);
        final Predicate<BlockState> isOpaque = Heightmap.Types.WORLD_SURFACE.isOpaque();
        // Rebuild directly from palette data using the heightmap type's predicate,
        // avoiding live section construction and block counting.
        for (int sectionIndex = sections.length - 1; sectionIndex >= 0; sectionIndex--) {
            final PalettedContainer<BlockState> section = sections[sectionIndex];
            if (empty[sectionIndex]) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    final int index = getIndex(x, z);
                    if (data.get(index) != 0) {
                        continue;
                    }
                    for (int y = 15; y >= 0; y--) {
                        if (isOpaque.test(section.get(x, y, z))) {
                            data.set(index, sectionIndex * 16 + y + 1);
                            break;
                        }
                    }
                }
            }
        }
        return new HeightmapSnapshot(data.getRaw(), heightAccessor);
    }

    public int getFirstAvailable(final int x, final int z) {
        return this.getFirstAvailable(getIndex(x, z));
    }

    private int getFirstAvailable(final int index) {
        return this.data.get(index) + this.heightAccessor.getMinY();
    }

    private static int getIndex(final int x, final int z) {
        return x + z * 16;
    }
}
