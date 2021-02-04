package net.pl3x.map.plugin.util.iterator;

import java.util.Iterator;

public abstract class AbstractSpiralIterator<T> implements Iterator<T> {
    Direction direction = Direction.RIGHT;
    int x, z, stepCount, stepLeg, legAxis, layer, totalSteps;

    protected AbstractSpiralIterator(int x, int z, int radius) {
        this.x = x;
        this.z = z;
        this.totalSteps = (radius * 2 + 1) * (radius * 2 + 1);
    }

    @Override
    public boolean hasNext() {
        return stepCount < totalSteps;
    }

    public int curStep() {
        return stepCount;
    }

    protected abstract T fromCoordinatePair(final int x, final int z);

    @Override
    public T next() {
        final T t = this.fromCoordinatePair(x, z);
        if (!hasNext()) {
            return t;
        }

        switch (direction) {
            case DOWN:
                z += 1;
                break;
            case LEFT:
                x -= 1;
                break;
            case UP:
                z -= 1;
                break;
            case RIGHT:
            default:
                x += 1;
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

    public enum Direction {
        RIGHT, DOWN, LEFT, UP;

        private static final Direction[] values = values();

        public Direction next() {
            return values[(this.ordinal() + 1) % values.length];
        }

        public static Direction of(int ordinal) {
            return values[ordinal];
        }
    }
}
