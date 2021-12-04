package net.pl3x.map.plugin.data;

import net.pl3x.map.plugin.util.Numbers;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public record ChunkCoordinate(int x, int z) {

    public int getRegionX() {
        return Numbers.chunkToRegion(this.x);
    }

    public int getRegionZ() {
        return Numbers.chunkToRegion(this.z);
    }

    public int getBlockX() {
        return Numbers.chunkToBlock(this.x);
    }

    public int getBlockZ() {
        return Numbers.chunkToBlock(this.z);
    }

    public Region regionCoordinate() {
        return new Region(this.getRegionX(), this.getRegionZ());
    }
}
