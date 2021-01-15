package net.pl3x.map.util.iterator;

import net.pl3x.map.data.Region;

public final class RegionSpiralIterator extends AbstractSpiralIterator<Region> {

    public RegionSpiralIterator(int x, int z, int radius) {
        super(x, z, radius);
    }

    @Override
    protected Region fromCoordinatePair(final int x, final int z) {
        return new Region(x, z);
    }
}
