package xyz.jpenilla.squaremap.common.util.chunksnapshot;

import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

final class HeightmapSnapshot {
    private final BitStorage data;
    private final LevelHeightAccessor heightAccessor;

    HeightmapSnapshot(
        final ChunkAccess chunk,
        final LevelHeightAccessor heightAccessor,
        final Heightmap.Types heightmapType
    ) {
        this.data = new SimpleBitStorage(
            Mth.ceillog2(heightAccessor.getHeight() + 1),
            256,
            chunk.getOrCreateHeightmapUnprimed(heightmapType).getRawData().clone()
        );
        this.heightAccessor = heightAccessor;
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
