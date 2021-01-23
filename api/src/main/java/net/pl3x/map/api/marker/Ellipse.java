package net.pl3x.map.api.marker;

import net.pl3x.map.api.Point;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * Ellipse marker
 */
public final class Ellipse extends Marker {

    private Point center;
    private double radiusX;
    private double radiusZ;

    Ellipse(final @NonNull Point center, final double radiusX, final double radiusZ) {
        this.center = center;
        this.radiusX = radiusX;
        this.radiusZ = radiusZ;
    }

    /**
     * Get the center point of this ellipse
     *
     * @return center point
     */
    public @NonNull Point center() {
        return this.center;
    }

    /**
     * Set a new center point for this ellipse
     *
     * @param center new center
     */
    public void center(final @NonNull Point center) {
        this.center = center;
    }

    /**
     * Get the radiusX of this ellipse
     *
     * @return radiusX
     */
    public double radiusX() {
        return this.radiusX;
    }

    /**
     * Set the radiusX of this ellipse
     *
     * @param radius new radiusX
     */
    public void radiusX(final double radius) {
        this.radiusX = radius;
    }

    /**
     * Get the radiusZ of this ellipse
     *
     * @return radiusZ
     */
    public double radiusZ() {
        return this.radiusZ;
    }

    /**
     * Set the radiusZ of this ellipse
     *
     * @param radius new radiusZ
     */
    public void radiusZ(final double radius) {
        this.radiusZ = radius;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Ellipse ellipse = (Ellipse) o;
        return Double.compare(ellipse.radiusX, this.radiusX) == 0
                && Double.compare(ellipse.radiusZ, this.radiusZ) == 0
                && this.center.equals(ellipse.center);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.center, this.radiusX, this.radiusZ);
    }

}
