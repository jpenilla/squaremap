package xyz.jpenilla.squaremap.plugin.util.iterator;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.plugin.data.RegionCoordinate;

@DefaultQualifier(NonNull.class)
public final class RegionSpiralIterator extends AbstractSpiralIterator<RegionCoordinate> {

    public RegionSpiralIterator(int x, int z, int radius) {
        super(x, z, radius);
    }

    @Override
    protected RegionCoordinate fromCoordinatePair(final int x, final int z) {
        return new RegionCoordinate(x, z);
    }
}
