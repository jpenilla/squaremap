package xyz.jpenilla.squaremap.common.coordinate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public record ChunkCoordinate(int x, int z) {

    public int getRegionX() {
        return CoordinateConversions.chunkToRegion(this.x);
    }

    public int getRegionZ() {
        return CoordinateConversions.chunkToRegion(this.z);
    }

    public int getBlockX() {
        return CoordinateConversions.chunkToBlock(this.x);
    }

    public int getBlockZ() {
        return CoordinateConversions.chunkToBlock(this.z);
    }

    public RegionCoordinate regionCoordinate() {
        return new RegionCoordinate(this.getRegionX(), this.getRegionZ());
    }
}
