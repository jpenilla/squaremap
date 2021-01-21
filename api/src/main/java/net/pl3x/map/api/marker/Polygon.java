package net.pl3x.map.api.marker;

import net.pl3x.map.api.Point;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Polygon marker
 */
public final class Polygon extends Marker implements IPolygon {

    private final List<Point> mainPolygon;
    private final List<List<Point>> negativeSpace;

    Polygon(final @NonNull List<Point> points, final @NonNull List<List<Point>> negativeSpace) {
        this.mainPolygon = new ArrayList<>(points);
        this.negativeSpace = new ArrayList<>(negativeSpace);
    }

    @Override
    public @NonNull List<List<Point>> negativeSpace() {
        return this.negativeSpace;
    }

    @Override
    public @NonNull List<Point> mainPolygon() {
        return this.mainPolygon;
    }

}
