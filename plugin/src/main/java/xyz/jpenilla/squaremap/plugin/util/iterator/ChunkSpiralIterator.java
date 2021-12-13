package xyz.jpenilla.squaremap.plugin.util.iterator;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.plugin.data.ChunkCoordinate;

@DefaultQualifier(NonNull.class)
public final class ChunkSpiralIterator extends AbstractSpiralIterator<ChunkCoordinate> {

    public ChunkSpiralIterator(int x, int z, int radius) {
        super(x, z, radius);
    }

    @Override
    protected ChunkCoordinate fromCoordinatePair(int x, int z) {
        return new ChunkCoordinate(x, z);
    }
}
