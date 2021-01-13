package net.pl3x.map.util.iterator;

import net.minecraft.server.v1_16_R3.ChunkCoordIntPair;

public final class ChunkSpiralIterator extends AbstractSpiralIterator<ChunkCoordIntPair> {

    public ChunkSpiralIterator(int x, int z, int radius) {
        super(x, z, radius);
    }

    @Override
    protected ChunkCoordIntPair fromCoordPair(int x, int z) {
        return new ChunkCoordIntPair(x, z);
    }
}
