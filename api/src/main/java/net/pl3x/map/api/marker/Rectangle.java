package net.pl3x.map.api.marker;

import net.pl3x.map.api.Point;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Rectangle marker
 */
public final class Rectangle extends Marker {

    private Point point1;
    private Point point2;

    Rectangle(final @NonNull Point point1, final @NonNull Point point2) {
        this.point1 = point1;
        this.point2 = point2;
    }

    public void points(final @NonNull Point point1, final @NonNull Point point2) {
        this.point1 = point1;
        this.point2 = point2;
    }

    /**
     * Get the first corner point for this rectangle
     *
     * @return point1
     */
    public @NonNull Point point1() {
        return this.point1;
    }

    /**
     * Get the second corner point for this rectangle
     *
     * @return point2
     */
    public @NonNull Point point2() {
        return this.point2;
    }

    /**
     * Set the first corner point for this rectangle
     *
     * @param point new point
     */
    public void point1(final @NonNull Point point) {
        this.point1 = point;
    }

    /**
     * Set the second corner point for this rectangle
     *
     * @param point new point
     */
    public void point2(final @NonNull Point point) {
        this.point2 = point;
    }

}
