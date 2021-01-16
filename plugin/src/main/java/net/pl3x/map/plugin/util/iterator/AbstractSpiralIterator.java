package net.pl3x.map.plugin.util.iterator;

import java.util.Iterator;

public abstract class AbstractSpiralIterator<T> implements Iterator<T> {
    private Direction direction = Direction.RIGHT;
    private int x, z, stepCount, stepLeg, legAxis, layer;
    private final int totalSteps;

    protected AbstractSpiralIterator(int x, int z, int radius) {
        this.x = x;
        this.z = z;
        this.totalSteps = (radius * 2 + 1) * (radius * 2 + 1);
    }

    @Override
    public boolean hasNext() {
        return stepCount < totalSteps;
    }

    protected abstract T fromCoordinatePair(final int x, final int z);

    @Override
    public T next() {
        final T t = this.fromCoordinatePair(x, z);
        if (!hasNext()) {
            return t;
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

        return t;
    }

    private enum Direction {
        RIGHT, DOWN, LEFT, UP;

        private static final Direction[] values = values();

        public Direction next() {
            return values[(this.ordinal() + 1) % values.length];
        }
    }
}
