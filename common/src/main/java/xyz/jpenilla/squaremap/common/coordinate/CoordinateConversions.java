package xyz.jpenilla.squaremap.common.coordinate;

public final class CoordinateConversions {
    private CoordinateConversions() {
    }

    public static int regionToBlock(int n) {
        return n << 9;
    }

    public static int blockToRegion(int n) {
        return n >> 9;
    }

    public static int regionToChunk(int n) {
        return n << 5;
    }

    public static int chunkToRegion(int n) {
        return n >> 5;
    }

    public static int chunkToBlock(int n) {
        return n << 4;
    }

    public static int blockToChunk(int n) {
        return n >> 4;
    }
}
