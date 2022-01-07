package xyz.jpenilla.squaremap.sponge.util;

import org.spongepowered.math.vector.Vector3i;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.util.Numbers;

public final class SpongeVectors {
    private SpongeVectors() {
    }

    public static ChunkCoordinate fromChunkPos(final Vector3i chunkPos) {
        return new ChunkCoordinate(chunkPos.x(), chunkPos.z());
    }

    public static ChunkCoordinate fromBlockPos(final Vector3i blockPos) {
        return new ChunkCoordinate(
            Numbers.blockToChunk(blockPos.x()),
            Numbers.blockToChunk(blockPos.z())
        );
    }
}
