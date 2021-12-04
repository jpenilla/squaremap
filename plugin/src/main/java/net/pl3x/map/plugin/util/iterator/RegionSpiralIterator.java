package net.pl3x.map.plugin.util.iterator;

import net.pl3x.map.plugin.data.Region;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class RegionSpiralIterator extends AbstractSpiralIterator<Region> {

    public RegionSpiralIterator(int x, int z, int radius) {
        super(x, z, radius);
    }

    @Override
    protected Region fromCoordinatePair(final int x, final int z) {
        return new Region(x, z);
    }
}
