package net.pl3x.map.util;

import java.util.Iterator;
import net.pl3x.map.data.Region;

public class SpiralIterator implements Iterator<Region> {
    private final int radius, step;
    private int x, z, legX, legZ, leg = 2;
    private boolean hasNext = true;

    public SpiralIterator(int x, int z, int radius) {
        this(x, z, radius, 1);
    }

    public SpiralIterator(int x, int z, int radius, int step) {
        this.x = x;
        this.z = z;
        this.radius = radius;
        this.step = step;
        this.legX = x + 1;
        this.legZ = z + 1;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public Region next() {
        final Region region = new Region(x, z);
        if (!hasNext) {
            return region;
        }
        if (leg == 0) {
            x += step;
            if (x >= legX) {
                leg++;
            }
        } else if (leg == 1) {
            z += step;
            if (z >= legZ) {
                leg++;
            }
        } else if (leg == 2) {
            x -= step;
            if (-x >= legX) {
                leg++;
                legX += step;
            }
        } else if (leg == 3) {
            z -= step;
            if (-z >= legZ) {
                leg = 0;
                legZ += step;
            }
        }
        if (legX > radius && legZ > radius) {
            hasNext = false;
        }
        return region;
    }
}
