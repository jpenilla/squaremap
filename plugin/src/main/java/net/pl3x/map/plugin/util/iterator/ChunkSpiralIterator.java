package net.pl3x.map.plugin.util.iterator;

import net.pl3x.map.plugin.data.ChunkCoordinate;

public final class ChunkSpiralIterator extends AbstractSpiralIterator<ChunkCoordinate> {

    public ChunkSpiralIterator(int x, int z, int radius) {
        super(x, z, radius);
    }

    @Override
    protected ChunkCoordinate fromCoordinatePair(int x, int z) {
        return new ChunkCoordinate(x, z);
    }
}
