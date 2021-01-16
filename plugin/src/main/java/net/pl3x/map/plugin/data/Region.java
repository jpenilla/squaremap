package net.pl3x.map.plugin.data;

import net.pl3x.map.plugin.util.Numbers;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class Region {
    private final int x;
    private final int z;

    public Region(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public int getChunkX() {
        return Numbers.regionToChunk(x);
    }

    public int getChunkZ() {
        return Numbers.regionToChunk(z);
    }

    public int getBlockX() {
        return Numbers.regionToBlock(x);
    }

    public int getBlockZ() {
        return Numbers.regionToBlock(z);
    }

    public @NonNull ChunkCoordinate chunkCoordinate() {
        return new ChunkCoordinate(this.getChunkX(), this.getChunkZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Region)) {
            return false;
        }
        Region other = (Region) o;
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
        return "Region{x=" + x + ",z=" + z + "}";
    }
}
