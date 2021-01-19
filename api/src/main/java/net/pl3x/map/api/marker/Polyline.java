package net.pl3x.map.api.marker;

import net.pl3x.map.api.Point;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * Polyline marker, used to draw any number of lines
 */
public final class Polyline extends Marker {

    private Point[][] points;

    Polyline(final @NonNull Point @NonNull [] @NonNull [] points) {
        this.points = points;
    }

    /**
     * Get the points that make up this polyline. The inner arrays each represent a line, with the outer array being the array of lines.
     * If only a single line is represented by this polyline, the outer array will be of size 1.
     *
     * @return points
     */
    public @NonNull Point @NonNull [] @NonNull [] points() {
        return this.points;
    }

    /**
     * Set a new list of points for this line
     *
     * @param points new points
     */
    public void points(final @NonNull List<Point> points) {
        this.points(points.toArray(Point[]::new));
    }

    /**
     * Set a new list of points for this line
     *
     * @param points new points
     */
    public void points(final @NonNull Point @NonNull ... points) {
        this.multiPoints(new Point[][]{points});
    }

    /**
     * Set a new list of lines for this multiline
     *
     * @param points new points
     */
    public void multiPoints(final @NonNull List<List<Point>> points) {
        this.multiPoints(points.stream().map(l -> l.toArray(Point[]::new)).toArray(Point[][]::new));
    }

    /**
     * Set a new list of lines for this multiline
     *
     * @param points new points
     */
    public void multiPoints(final @NonNull Point @NonNull [] @NonNull ... points) {
        this.points = points;
    }

}
