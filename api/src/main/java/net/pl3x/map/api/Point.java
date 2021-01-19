package net.pl3x.map.api;

import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

/**
 * Represents a point on a map in the XZ plane. May be relative or absolute depending on the context
 */
public final class Point {

    private final double x;
    private final double z;

    private Point(double x, double z) {
        this.x = x;
        this.z = z;
    }

    /**
     * Get the x position of this point
     *
     * @return x
     */
    public double x() {
        return this.x;
    }

    /**
     * Get the z position of this point
     *
     * @return z
     */
    public double z() {
        return this.z;
    }

    /**
     * Create a new point from an x and z position
     *
     * @param x x position
     * @param z z position
     * @return point
     */
    public static @NonNull Point of(final double x, final double z) {
        return new Point(x, z);
    }

    /**
     * Create a new point from an x and z position
     *
     * @param x x position
     * @param z z position
     * @return point
     */
    public static @NonNull Point point(final double x, final double z) {
        return new Point(x, z);
    }

    /**
     * Get a new point from a Bukkit {@link Location}. Uses precise location
     *
     * @param location location
     * @return point
     */
    public static @NonNull Point fromLocation(final @NonNull Location location) {
        return new Point(location.getX(), location.getZ());
    }

    /**
     * Get a new point from a Bukkit {@link Location}. Uses block location
     *
     * @param location location
     * @return point
     */
    public static @NonNull Point fromBlockLocation(final @NonNull Location location) {
        return new Point(location.getBlockX(), location.getBlockZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point that = (Point) o;
        return Double.compare(that.x, this.x) == 0 && Double.compare(that.z, this.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.z);
    }

}
