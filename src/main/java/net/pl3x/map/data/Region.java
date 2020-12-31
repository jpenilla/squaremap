package net.pl3x.map.data;

public class Region {
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
        return x << 5;
    }

    public int getChunkZ() {
        return z << 5;
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
