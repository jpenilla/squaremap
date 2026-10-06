package xyz.jpenilla.squaremap.sponge.util;

import org.spongepowered.math.vector.Vector3i;
import xyz.jpenilla.squaremap.common.coordinate.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.coordinate.CoordinateConversions;

public final class SpongeVectors {
    private SpongeVectors() {
    }

    public static ChunkCoordinate fromChunkPos(final Vector3i chunkPos) {
        return new ChunkCoordinate(chunkPos.x(), chunkPos.z());
    }

    public static ChunkCoordinate fromBlockPos(final Vector3i blockPos) {
        return new ChunkCoordinate(
            CoordinateConversions.blockToChunk(blockPos.x()),
            CoordinateConversions.blockToChunk(blockPos.z())
        );
    }
}
