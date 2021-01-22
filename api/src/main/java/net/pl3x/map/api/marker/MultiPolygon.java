package net.pl3x.map.api.marker;

import net.pl3x.map.api.Point;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * MultiPolygon marker, used to display multiple polygons while treating them as a single marker
 */
public final class MultiPolygon extends Marker {

    private final List<MultiPolygonPart> subPolygons;

    MultiPolygon(final @NonNull List<MultiPolygonPart> subPolygons) {
        this.subPolygons = new ArrayList<>(subPolygons);
    }

    /**
     * Create a new {@link MultiPolygonPart} from a list of points
     *
     * @param points points
     * @return new part
     */
    public static @NonNull MultiPolygonPart part(final @NonNull Point @NonNull ... points) {
        return part(Arrays.asList(points), Collections.emptyList());
    }

    /**
     * Create a new {@link MultiPolygonPart} from a list of points, and a secondary list polygons that make up the negative space
     *
     * @param points        points
     * @param negativeSpace negative space
     * @return new part
     */
    @SafeVarargs
    public static @NonNull MultiPolygonPart part(final @NonNull List<Point> points, final @NonNull List<Point> @NonNull ... negativeSpace) {
        return part(points, Arrays.asList(negativeSpace));
    }

    /**
     * Create a new {@link MultiPolygonPart} from a list of points, and a secondary list polygons that make up the negative space
     *
     * @param points        points
     * @param negativeSpace negative space
     * @return new part
     */
    public static @NonNull MultiPolygonPart part(final @NonNull List<Point> points, final @NonNull List<List<Point>> negativeSpace) {
        return new MultiPolygonPart(points, negativeSpace);
    }

    /**
     * Get the mutable list of the sub polygons which make up this MultiPolygon
     *
     * @return sub polygons
     */
    public @NonNull List<MultiPolygonPart> subPolygons() {
        return this.subPolygons;
    }

    /**
     * Set a new list of sub polygons. Will remove existing sub polygons.
     *
     * @param subPolygons new sub polygons
     */
    public void subPolygons(final @NonNull List<MultiPolygonPart> subPolygons) {
        this.subPolygons.clear();
        this.subPolygons.addAll(subPolygons);
    }

    /**
     * Set a new list of sub polygons. Will remove existing sub polygons.
     *
     * @param subPolygons new sub polygons
     */
    public void subPolygons(final @NonNull MultiPolygonPart @NonNull ... subPolygons) {
        this.subPolygons(Arrays.asList(subPolygons));
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MultiPolygon that = (MultiPolygon) o;
        return this.subPolygons.equals(that.subPolygons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.subPolygons);
    }

    /**
     * MultiPolygonPart is a class to represent sub polygons of {@link MultiPolygon MultiPolygons}
     */
    public static final class MultiPolygonPart implements IPolygon {

        private final List<Point> mainPolygon;
        private final List<List<Point>> negativeSpace;

        private MultiPolygonPart(final @NonNull List<Point> points, final @NonNull List<List<Point>> negativeSpace) {
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

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final MultiPolygonPart that = (MultiPolygonPart) o;
            return this.mainPolygon.equals(that.mainPolygon)
                    && this.negativeSpace.equals(that.negativeSpace);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.mainPolygon, this.negativeSpace);
        }

    }

}
