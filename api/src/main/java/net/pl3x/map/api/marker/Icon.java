package net.pl3x.map.api.marker;

import net.pl3x.map.api.Key;
import net.pl3x.map.api.Pl3xMap;
import net.pl3x.map.api.Point;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * Icon marker, used to create an icon with an image on a map
 */
public final class Icon extends Marker {

    private Point point;
    private Point tooltipAnchor;
    private Point anchor;
    private Key image;
    private int sizeX;
    private int sizeZ;

    Icon(
            final @NonNull Point point,
            final @NonNull Point tooltipAnchor,
            final @NonNull Point anchor,
            final @NonNull Key image,
            final int sizeX,
            final int sizeZ
    ) {
        this.point = point;
        this.tooltipAnchor = tooltipAnchor;
        this.anchor = anchor;
        this.image = image;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
    }

    /**
     * Get the point where this icon will be located at
     *
     * @return point
     */
    public @NonNull Point point() {
        return this.point;
    }

    /**
     * Get the coordinates of the point from which popups will "open", relative to the icon anchor.
     *
     * @return point
     */
    public @NonNull Point tooltipAnchor() {
        return this.tooltipAnchor;
    }

    /**
     * Get the coordinates of the "tip" of the icon (relative to its top left corner).
     * The icon will be aligned so that this point is at the marker's geographical location.
     * Centered by default if size is specified.
     *
     * @return point
     */
    public @NonNull Point anchor() {
        return this.anchor;
    }

    /**
     * Get the key for the currently used image
     *
     * @return key
     */
    public @NonNull Key image() {
        return this.image;
    }

    /**
     * Get the x size
     *
     * @return x size
     */
    public int sizeX() {
        return this.sizeX;
    }

    /**
     * Get the z size
     *
     * @return z size
     */
    public int sizeZ() {
        return this.sizeZ;
    }

    /**
     * Set a new location for this icon marker
     *
     * @param point new point
     */
    public void point(final @NonNull Point point) {
        this.point = point;
    }

    /**
     * Set the coordinates of the point from which popups will "open", relative to the icon anchor.
     *
     * @param tooltipAnchor new point
     */
    public void tooltipAnchor(final @NonNull Point tooltipAnchor) {
        this.tooltipAnchor = tooltipAnchor;
    }

    /**
     * Set the coordinates of the "tip" of the icon (relative to its top left corner).
     * The icon will be aligned so that this point is at the marker's geographical location.
     * Centered by default if size is specified.
     *
     * @param anchor new point
     */
    public void anchor(final @NonNull Point anchor) {
        this.anchor = anchor;
    }

    /**
     * Set the image to use for this icon. Must be registered with the icon registry
     *
     * @param image new image
     * @see Pl3xMap#iconRegistry()
     */
    public void image(final @NonNull Key image) {
        this.image = image;
    }

    /**
     * Set the x size
     *
     * @param size new size
     */
    public void sizeX(final int size) {
        this.sizeX = size;
    }

    /**
     * Set the z size
     *
     * @param size new size
     */
    public void sizeZ(final int size) {
        this.sizeZ = size;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Icon icon = (Icon) o;
        return this.sizeX == icon.sizeX
                && this.sizeZ == icon.sizeZ
                && this.point.equals(icon.point)
                && this.tooltipAnchor.equals(icon.tooltipAnchor)
                && this.anchor.equals(icon.anchor)
                && this.image.equals(icon.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.point, this.tooltipAnchor, this.anchor, this.image, this.sizeX, this.sizeZ);
    }

}
