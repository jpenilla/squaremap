package net.pl3x.map.api.marker;

import net.pl3x.map.api.Point;
import net.pl3x.map.api.marker.MultiPolygon.MultiPolygonPart;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.List;

/**
 * Interface with common methods to {@link Polygon} and {@link MultiPolygonPart}
 */
public interface IPolygon {

    /**
     * Get the mutable list of polygons which make up the negative space for this polygon.
     *
     * @return negative space
     */
    @NonNull List<List<Point>> negativeSpace();

    /**
     * Set the negative space for this polygon. This will reset any negative space currently set.
     *
     * @param points new negative space
     */
    @SuppressWarnings("unchecked")
    default void negativeSpace(final @NonNull List<Point> @NonNull ... points) {
        this.negativeSpace(Arrays.asList(points));
    }

    /**
     * Set the negative space for this polygon. This will reset any negative space currently set.
     *
     * @param points new negative space
     */
    default void negativeSpace(final @NonNull List<List<Point>> points) {
        this.negativeSpace().clear();
        this.negativeSpace().addAll(points);
    }

    /**
     * Get the mutable list of the points which make up the main polygon
     *
     * @return main polygon
     */
    @NonNull List<Point> mainPolygon();

    /**
     * Set the points which make up the main polygon for this polygon. This will reset any currently set points.
     *
     * @param points new main polygon
     */
    default void mainPolygon(final @NonNull Point @NonNull ... points) {
        this.mainPolygon(Arrays.asList(points));
    }

    /**
     * Set the points which make up the main polygon for this polygon. This will reset any currently set points.
     *
     * @param points new main polygon
     */
    default void mainPolygon(final @NonNull List<Point> points) {
        this.mainPolygon().clear();
        this.mainPolygon().addAll(points);
    }

}
