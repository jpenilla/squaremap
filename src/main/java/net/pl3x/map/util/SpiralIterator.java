package net.pl3x.map.util;

import net.pl3x.map.data.Region;

import java.util.Iterator;

public class SpiralIterator implements Iterator<Region> {
    private Direction direction = Direction.RIGHT;
    private int x, z, stepCount, stepLeg, legAxis, layer;
    private final int totalSteps;

    public SpiralIterator(int x, int z, int radius) {
        this.x = x;
        this.z = z;
        this.totalSteps = (radius * 2 + 1) * (radius * 2 + 1);
    }

    @Override
    public boolean hasNext() {
        return stepCount < totalSteps;
    }

    public Region next() {
        final Region region = new Region(x, z);
        if (!hasNext()) {
            return region;
        }

        switch (direction) {
            case RIGHT:
                x += 1;
                break;
            case DOWN:
                z += 1;
                break;
            case LEFT:
                x -= 1;
                break;
            case UP:
                z -= 1;
                break;
        }

        stepCount++;
        stepLeg++;
        if (stepLeg > layer) {
            direction = direction.next();
            stepLeg = 0;
            legAxis++;
            if (legAxis > 1) {
                legAxis = 0;
                layer++;
            }
        }

        return region;
    }

    private enum Direction {
        RIGHT, DOWN, LEFT, UP;

        private static final Direction[] values = values();

        public Direction next() {
            return values[(this.ordinal() + 1) % values.length];
        }
    }
}
