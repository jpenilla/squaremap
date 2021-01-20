package net.pl3x.map.api;

import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * Represents a point on a map in the XZ plane. May be relative or absolute depending on the context
 */
public final class Point {

    private final int x;
    private final int z;

    private Point(final int x, final int z) {
        this.x = x;
        this.z = z;
    }

    /**
     * Get the x position of this point
     *
     * @return x
     */
    public int x() {
        return this.x;
    }

    /**
     * Get the z position of this point
     *
     * @return z
     */
    public int z() {
        return this.z;
    }

    /**
     * Create a new point from an x and z position
     *
     * @param x x position
     * @param z z position
     * @return point
     */
    public static @NonNull Point of(final int x, final int z) {
        return new Point(x, z);
    }

    /**
     * Create a new point from an x and z position
     *
     * @param x x position
     * @param z z position
     * @return point
     */
    public static @NonNull Point point(final int x, final int z) {
        return new Point(x, z);
    }

    /**
     * Get a new point from a Bukkit {@link Location}. Uses block location
     *
     * @param location location
     * @return point
     */
    public static @NonNull Point fromLocation(final @NonNull Location location) {
        return point(location.getBlockX(), location.getBlockZ());
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point that = (Point) o;
        return that.x == this.x && that.z == this.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.z);
    }

}
