package xyz.jpenilla.squaremap.common.util;

import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;

@DefaultQualifier(NonNull.class)
public final class SpiralIterator<T> implements Iterator<T> {
    private final long totalSteps;
    private final CoordinateMapper<T> coordinateMapper;

    private int x;
    private int z;
    private long legAxis;
    private long stepCount;
    private long stepLeg;
    private long layer;
    private Direction direction = Direction.RIGHT;

    private SpiralIterator(final CoordinateMapper<T> coordinateMapper, final int x, final int z, final int radius) {
        this.coordinateMapper = coordinateMapper;
        this.x = x;
        this.z = z;
        this.totalSteps = (radius * 2L + 1) * (radius * 2L + 1);
    }

    @Override
    public boolean hasNext() {
        return this.stepCount < this.totalSteps;
    }

    @Override
    public T next() {
        final T t = this.coordinateMapper.create(this.x, this.z);
        if (!this.hasNext()) {
            return t;
        }

        switch (this.direction) {
            case DOWN -> this.z++;
            case LEFT -> this.x--;
            case UP -> this.z--;
            case RIGHT -> this.x++;
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

    public static <T> Iterator<T> create(final CoordinateMapper<T> coordinateMapper, final int x, final int z, final int radius) {
        return new SpiralIterator<>(coordinateMapper, x, z, radius);
    }

    public static Iterator<ChunkCoordinate> chunk(final int x, final int z, final int radius) {
        return create(ChunkCoordinate::new, x, z, radius);
    }

    public static Iterator<RegionCoordinate> region(final int x, final int z, final int radius) {
        return create(RegionCoordinate::new, x, z, radius);
    }

    @FunctionalInterface
    public interface CoordinateMapper<T> {
        T create(int x, int z);
    }

    private enum Direction {
        RIGHT,
        DOWN,
        LEFT,
        UP;

        private static final Direction[] VALUES = values();

        public Direction next() {
            return VALUES[(this.ordinal() + 1) % VALUES.length];
        }
    }
}
