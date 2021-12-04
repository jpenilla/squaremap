package net.pl3x.map.plugin.util.iterator;

import net.pl3x.map.plugin.data.RegionCoordinate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

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
