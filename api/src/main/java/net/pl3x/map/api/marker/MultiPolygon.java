package net.pl3x.map.api.marker;

import net.pl3x.map.api.Point;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MultiPolygon extends Marker {

    private final List<MultiPolygonPart> subPolygons;

    MultiPolygon(final @NonNull List<MultiPolygonPart> subPolygons) {
        this.subPolygons = new ArrayList<>(subPolygons);
    }

    public static @NonNull MultiPolygonPart subPolygon(final @NonNull Point @NonNull ... points) {
        return new MultiPolygonPart(points, Collections.emptyList());
    }

    public static @NonNull MultiPolygonPart subPolygon(final @NonNull List<Point> points, final @NonNull List<Point @NonNull []> negativeSpace) {
        return new MultiPolygonPart(points.toArray(Point[]::new), negativeSpace);
    }

    public static @NonNull MultiPolygonPart subPolygon(final @NonNull Point @NonNull [] points, final @NonNull List<Point @NonNull []> negativeSpace) {
        return new MultiPolygonPart(points, negativeSpace);
    }

    public static @NonNull MultiPolygonPart subPolygon(final @NonNull Point @NonNull [] points, final @NonNull Point @NonNull [] @NonNull ... negativeSpace) {
        return new MultiPolygonPart(points, Arrays.asList(negativeSpace));
    }

    public @NonNull List<MultiPolygonPart> subPolygons() {
        return this.subPolygons;
    }

    public static final class MultiPolygonPart {

        private Point[] mainPolygon;
        private final List<Point[]> negativeSpace;

        private MultiPolygonPart(final @NonNull Point @NonNull [] points, final @NonNull List<Point @NonNull []> negativeSpace) {
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

}
