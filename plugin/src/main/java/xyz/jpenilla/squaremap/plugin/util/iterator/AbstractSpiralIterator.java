package xyz.jpenilla.squaremap.plugin.util.iterator;

import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public abstract class AbstractSpiralIterator<T> implements Iterator<T> {
    private final long totalSteps;

    private int x;
    private int z;
    private long legAxis;
    private long stepCount;
    private long stepLeg;
    private long layer;
    private Direction direction = Direction.RIGHT;

    protected AbstractSpiralIterator(int x, int z, int radius) {
        this.x = x;
        this.z = z;
        this.totalSteps = (radius * 2L + 1) * (radius * 2L + 1);
    }

    @Override
    public boolean hasNext() {
        return this.stepCount < this.totalSteps;
    }

    protected abstract T fromCoordinatePair(final int x, final int z);

    @Override
    public T next() {
        final T t = this.fromCoordinatePair(this.x, this.z);
        if (!this.hasNext()) {
            return t;
        }

        switch (this.direction) {
            case DOWN -> this.z += 1;
            case LEFT -> this.x -= 1;
            case UP -> this.z -= 1;
            case RIGHT -> this.x += 1;
        }

        this.stepCount++;
        this.stepLeg++;
        if (this.stepLeg > this.layer) {
            this.direction = this.direction.next();
            this.stepLeg = 0;
            this.legAxis++;
            if (this.legAxis > 1) {
                this.legAxis = 0;
                this.layer++;
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
    }
}
