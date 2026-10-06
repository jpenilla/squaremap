package xyz.jpenilla.squaremap.common.coordinate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public record RegionCoordinate(int x, int z) {

    public int getChunkX() {
        return CoordinateConversions.regionToChunk(this.x);
    }

    public int getChunkZ() {
        return CoordinateConversions.regionToChunk(this.z);
    }

    public int getBlockX() {
        return CoordinateConversions.regionToBlock(this.x);
    }

    public int getBlockZ() {
        return CoordinateConversions.regionToBlock(this.z);
    }

    public ChunkCoordinate chunkCoordinate() {
        return new ChunkCoordinate(this.getChunkX(), this.getChunkZ());
    }
}
