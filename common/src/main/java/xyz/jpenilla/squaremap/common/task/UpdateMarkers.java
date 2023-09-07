package xyz.jpenilla.squaremap.common.task;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.awt.Color;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.marker.Circle;
import xyz.jpenilla.squaremap.api.marker.Ellipse;
import xyz.jpenilla.squaremap.api.marker.Icon;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;
import xyz.jpenilla.squaremap.api.marker.MultiPolygon;
import xyz.jpenilla.squaremap.api.marker.Polygon;
import xyz.jpenilla.squaremap.api.marker.Polyline;
import xyz.jpenilla.squaremap.api.marker.Rectangle;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.FileUtil;

public final class UpdateMarkers implements Runnable {
    private final MapWorldInternal mapWorld;
    private final Object2LongMap<Key> lastUpdatedTime = new Object2LongOpenHashMap<>();
    private final Map<Key, Map<String, Object>> layerCache = new HashMap<>();
    private final Map<Key, Map<String, Object>> serializedLayerCache = new HashMap<>();
    private long lastResetTime = Long.MIN_VALUE; // min value to ensure initial write even with no layers

    public UpdateMarkers(final @NonNull MapWorldInternal mapWorld) {
        this.mapWorld = mapWorld;
    }

    @Override
    public void run() {
        final Registry<LayerProvider> layerRegistry = this.mapWorld.layerRegistry();

        final List<Map<String, Object>> layers = new ArrayList<>();
        final Set<Key> layerKeys = new HashSet<>();
        boolean[] changed = {false};
        layerRegistry.entries().forEach(registeredLayer -> {
            final LayerProvider provider = registeredLayer.right();
            final Key key = registeredLayer.left();
            layerKeys.add(key);
            final List<Marker> markers = List.copyOf(provider.getMarkers());

            final Map<String, Object> current = this.createMap(key, provider);
            current.put("markers", markers.hashCode());

            final Map<String, Object> previous = this.layerCache.get(key);

            if (previous == null || !previous.equals(current)) {
                changed[0] = true; // new or changed layer
                this.layerCache.put(key, current);

                final Map<String, Object> serializedLayer = this.serializeLayer(key, provider, markers);
                this.serializedLayerCache.put(key, serializedLayer);

                final long time = System.currentTimeMillis();
                this.lastUpdatedTime.put(key, time);

                final Map<String, Object> timeStampedLayer = new HashMap<>(serializedLayer);
                timeStampedLayer.put("timestamp", time);
                layers.add(timeStampedLayer);
            } else {
                final Map<String, Object> serializedLayer = this.serializedLayerCache.get(key);
                final long lastUpdate = this.lastUpdatedTime.getLong(key);

                final Map<String, Object> timeStampedLayer = new HashMap<>(serializedLayer);
                timeStampedLayer.put("timestamp", lastUpdate);
                layers.add(timeStampedLayer);
            }
        });

        // set flag to ensure update on removed layers
        changed[0] |= clearUnused(layerKeys, this.layerCache);
        changed[0] |= clearUnused(layerKeys, this.serializedLayerCache);
        changed[0] |= clearUnused(layerKeys, this.lastUpdatedTime);

        if (changed[0] || this.mapWorld.lastReset() != this.lastResetTime) {
            this.lastResetTime = this.mapWorld.lastReset();
            final Path file = this.mapWorld.tilesPath().resolve("markers.json");
            FileUtil.atomicWriteJsonAsync(file, layers);
        }
    }

    @SuppressWarnings("rawtypes")
    private static <K> boolean clearUnused(final Set<K> keepKeys, final Map<K, ?> map) {
        boolean madeChange = false;
        for (final K key : Set.copyOf(map.keySet())) {
            if (!keepKeys.contains(key)) {
                madeChange = true;
                if (map instanceof Object2LongMap longMap) {
                    longMap.removeLong(key);
                } else {
                    map.remove(key);
                }
            }
        }
        return madeChange;
    }

    private @NonNull Map<String, Object> serializeLayer(final @NonNull Key key, final @NonNull LayerProvider provider, final @NonNull List<Marker> markers) {
        final Map<String, Object> map = this.createMap(key, provider);
        map.put("markers", this.serializeMarkers(markers));
        return map;
    }

    private Map<String, Object> createMap(Key key, LayerProvider provider) {
        final Map<String, Object> map = new HashMap<>();
        map.put("id", key.getKey());
        map.put("name", provider.getLabel());
        map.put("control", provider.showControls());
        map.put("hide", provider.defaultHidden());
        map.put("order", provider.layerPriority());
        map.put("z_index", provider.zIndex());
        return map;
    }

    private @NonNull List<Map<String, Object>> serializeMarkers(final @NonNull Collection<Marker> markers) {
        final List<Map<String, Object>> processed = new ArrayList<>();

        for (final Marker marker : markers) {
            final Map<String, Object> markerMap = new HashMap<>();
            populateOptions(markerMap, marker.markerOptions());
            serialize(marker, markerMap);
            processed.add(markerMap);
        }

        return processed;
    }

    private static void populateOptions(final @NonNull Map<String, Object> marker, final @NonNull MarkerOptions options) {
        final MarkerOptions defaults = MarkerOptions.defaultOptions();
        if (options.stroke() != defaults.stroke()) {
            marker.put("stroke", options.stroke());
        }
        if (!options.strokeColor().equals(defaults.strokeColor())) {
            marker.put("color", toHexString(options.strokeColor()));
        }
        if (options.strokeWeight() != defaults.strokeWeight()) {
            marker.put("weight", options.strokeWeight());
        }
        if (options.strokeOpacity() != defaults.strokeOpacity()) {
            marker.put("opacity", options.strokeOpacity());
        }
        if (options.fill() != defaults.fill()) {
            marker.put("fill", options.fill());
        }
        final Color fillColor = options.fillColor();
        if (fillColor != null) {
            marker.put("fillColor", toHexString(fillColor));
        }
        if (options.fillOpacity() != defaults.fillOpacity()) {
            marker.put("fillOpacity", options.fillOpacity());
        }
        if (options.fillRule() != defaults.fillRule()) {
            marker.put("fillRule", options.fillRule().toString().toLowerCase(Locale.ENGLISH));
        }
        final String clickTooltip = options.clickTooltip();
        if (clickTooltip != null) {
            marker.put("popup", clickTooltip);
        }
        final String hoverTooltip = options.hoverTooltip();
        if (hoverTooltip != null) {
            marker.put("tooltip", hoverTooltip);
        }
    }

    private static @NonNull String toHexString(final @NonNull Color color) {
        return "#" + Integer.toHexString(color.getRGB()).substring(2);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Marker> void serialize(final @NonNull T marker, final @NonNull Map<String, Object> destination) {
        final Class<? extends Marker> markerClass = marker.getClass();
        final MarkerSerializer<T> markerSerializer = (MarkerSerializer<T>) serializers.get(markerClass);
        if (markerSerializer == null) {
            throw new IllegalStateException("unknown marker type! no serializer present for " + markerClass.getName());
        }
        markerSerializer.serialize(destination, marker);
    }

    private static final Map<Class<? extends Marker>, MarkerSerializer<?>> serializers = new HashMap<>();

    private static <T extends Marker> void register(final @NonNull Class<T> markerClass, final @NonNull MarkerSerializer<T> serializer) {
        serializers.put(markerClass, serializer);
    }

    static {
        register(Polyline.class, (destination, line) -> {
            destination.put("type", "polyline");
            final Object points;
            if (line.points().size() == 1) {
                points = line.points().get(0).stream()
                    .map(UpdateMarkers::toMap)
                    .toList();
            } else {
                points = serializePoints(line.points().stream());
            }
            destination.put("points", points);
        });

        register(Rectangle.class, (destination, rectangle) -> {
            destination.put("type", "rectangle");
            destination.put("points", List.of(
                toMap(rectangle.point1()),
                toMap(rectangle.point2())
            ));
        });

        register(Circle.class, (destination, circle) -> {
            destination.put("type", "circle");
            destination.put("center", toMap(circle.center()));
            destination.put("radius", circle.radius());
        });

        register(Ellipse.class, (destination, ellipse) -> {
            destination.put("type", "ellipse");
            destination.put("center", toMap(ellipse.center()));
            destination.put("radiusX", ellipse.radiusX());
            destination.put("radiusZ", ellipse.radiusZ());
        });

        register(Polygon.class, (destination, polygon) -> {
            destination.put("type", "polygon");
            final List<List<Point>> list = new ArrayList<>(Collections.singleton(polygon.mainPolygon()));
            list.addAll(polygon.negativeSpace());
            destination.put(
                "points",
                serializePoints(list.stream())
            );
        });

        register(MultiPolygon.class, (destination, multiPolygon) -> {
            destination.put("type", "polygon");
            destination.put(
                "points",
                multiPolygon.subPolygons().stream().map(subPoly -> {
                    final List<List<Point>> list = new ArrayList<>(Collections.singleton(subPoly.mainPolygon()));
                    list.addAll(subPoly.negativeSpace());
                    return serializePoints(list.stream());
                }).toList()
            );
        });

        register(Icon.class, (destination, icon) -> {
            destination.put("type", "icon");
            destination.put("point", toMap(icon.point()));
            destination.put("size", toMap(Point.of(icon.sizeX(), icon.sizeZ())));
            destination.put("anchor", toMap(icon.anchor()));
            destination.put("tooltip_anchor", toMap(icon.tooltipAnchor()));
            destination.put("icon", icon.image().getKey());
        });
    }

    private static @NonNull List<List<Map<String, Integer>>> serializePoints(final @NonNull Stream<List<Point>> stream) {
        return stream.map(points ->
            points.stream().map(UpdateMarkers::toMap).toList()
        ).toList();
    }

    @FunctionalInterface
    private interface MarkerSerializer<T extends Marker> {
        void serialize(@NonNull Map<String, Object> destination, @NonNull T marker);
    }

    private static @NonNull Map<String, Integer> toMap(final @NonNull Point point) {
        return Map.of("x", (int) point.x(), "z", (int) point.z());
    }
}
