package net.pl3x.map.api.marker;

import net.pl3x.map.api.Point;
import org.checkerframework.checker.nullness.qual.NonNull;

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

}
