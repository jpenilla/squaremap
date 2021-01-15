package net.pl3x.map.data;

import net.pl3x.map.util.Numbers;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class ChunkCoordinate {
    private final int x;
    private final int z;

    public ChunkCoordinate(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public int getRegionX() {
        return Numbers.chunkToRegion(x);
    }

    public int getRegionZ() {
        return Numbers.chunkToRegion(z);
    }

    public int getBlockX() {
        return Numbers.chunkToBlock(x);
    }

    public int getBlockZ() {
        return Numbers.chunkToBlock(z);
    }

    public @NonNull Region regionCoordinate() {
        return new Region(this.getRegionX(), this.getRegionZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChunkCoordinate)) {
            return false;
        }
        ChunkCoordinate other = (ChunkCoordinate) o;
        return this.x == other.x && this.z == other.z;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + z;
        return result;
    }

    @Override
    public String toString() {
        return "Chunk{x=" + x + ",z=" + z + "}";
    }
}
