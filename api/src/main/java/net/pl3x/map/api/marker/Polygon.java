package net.pl3x.map.api.marker;

import net.pl3x.map.api.Point;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class Polygon extends Marker {

    private Point[] mainPolygon;
    private final List<Point[]> negativeSpace;

    Polygon(final @NonNull Point @NonNull [] points, final @NonNull List<Point @NonNull []> negativeSpace) {
        this.mainPolygon = points;
        this.negativeSpace = new ArrayList<>(negativeSpace);
    }

    public @NonNull List<Point @NonNull []> negativeSpace() {
        return this.negativeSpace;
    }

    public @NonNull Point @NonNull [] mainPolygon() {
        return this.mainPolygon;
    }

    public void mainPolygon(final @NonNull Point @NonNull ... mainPolygon) {
        this.mainPolygon = mainPolygon;
    }

}
