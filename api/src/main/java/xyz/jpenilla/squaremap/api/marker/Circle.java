package xyz.jpenilla.squaremap.api.marker;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.api.Point;

/**
 * Circle marker
 */
public final class Circle extends Marker {

    private Point center;
    private double radius;

    Circle(final @NonNull Point center, final double radius) {
        this.center = center;
        this.radius = radius;
    }

    /**
     * Get the center point of this circle
     *
     * @return center point
     */
    public @NonNull Point center() {
        return this.center;
    }

    /**
     * Set a new center point for this circle
     *
     * @param center new center
     */
    public void center(final @NonNull Point center) {
        this.center = center;
    }

    /**
     * Get the radius of this circle
     *
     * @return radius
     */
    public double radius() {
        return this.radius;
    }

    /**
     * Set the radius of this circle
     *
     * @param radius new radius
     */
    public void radius(double radius) {
        this.radius = radius;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final @Nullable Circle circle = (Circle) o;
        return this.markerOptionsMatch(circle)
            && Double.compare(circle.radius, this.radius) == 0
            && this.center.equals(circle.center);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.markerOptions(), this.center, this.radius);
    }

}
