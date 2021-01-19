package net.pl3x.map.plugin.task;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import net.pl3x.map.api.Key;
import net.pl3x.map.api.LayerProvider;
import net.pl3x.map.api.Point;
import net.pl3x.map.api.Registry;
import net.pl3x.map.api.marker.Circle;
import net.pl3x.map.api.marker.Icon;
import net.pl3x.map.api.marker.Marker;
import net.pl3x.map.api.marker.MarkerOptions;
import net.pl3x.map.api.marker.MultiPolygon;
import net.pl3x.map.api.marker.Polygon;
import net.pl3x.map.api.marker.Polyline;
import net.pl3x.map.api.marker.Rectangle;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.WorldManager;
import net.pl3x.map.plugin.util.FileUtil;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.awt.Color;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class UpdateMarkers extends BukkitRunnable {

    private final Pl3xMapPlugin plugin;
    private final WorldManager worldManager;
    private final Gson gson = new Gson();

    public UpdateMarkers(final @NonNull Pl3xMapPlugin plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.worldManager();
    }

    @Override
    public void run() {
        this.worldManager.worlds().values().forEach(enabledWorld -> {
            final Registry<LayerProvider> layerRegistry = enabledWorld.layerRegistry();

            List<Map<String, Object>> layers = new ArrayList<>();
            layerRegistry.entries().forEach(registeredLayer -> {
                final LayerProvider provider = registeredLayer.right();
                final Key key = registeredLayer.left();

                final Map<String, Object> layerMap = new HashMap<>();
                layerMap.put("id", key.getKey());
                layerMap.put("name", provider.getLabel());
                layerMap.put("control", provider.showControls());
                layerMap.put("hide", provider.defaultHidden());
                layerMap.put("order", provider.layerPriority());
                layerMap.put("markers", this.serializeMarkers(ImmutableList.copyOf(provider.getMarkers())));

                layers.add(layerMap);
            });

            final Path file = FileUtil.getWorldFolder(enabledWorld.bukkit()).resolve("markers.json");
            FileUtil.write(gson.toJson(layers), file);
        });
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
            if (line.points().length == 1) {
                points = Arrays.stream(line.points()[0])
                        .map(point -> toMap(point))
                        .collect(Collectors.toList());
            } else {
                points = serializePoints(Arrays.stream(line.points()));
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

        register(Polygon.class, (destination, polygon) -> {
            destination.put("type", "polygon");
            final List<Point[]> list = new ArrayList<>(Collections.singleton(polygon.mainPolygon()));
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
                        final List<Point[]> list = new ArrayList<>(Collections.singleton(subPoly.mainPolygon()));
                        list.addAll(subPoly.negativeSpace());
                        return serializePoints(list.stream());
                    }).collect(Collectors.toList())
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

    private static @NonNull List<List<Map<String, Double>>> serializePoints(final @NonNull Stream<Point[]> stream) {
        return stream.map(pointArray ->
                Arrays.stream(pointArray)
                        .map(point -> toMap(point))
                        .collect(Collectors.toList())
        ).collect(Collectors.toList());
    }

    @FunctionalInterface
    private interface MarkerSerializer<T extends Marker> {
        void serialize(@NonNull Map<String, Object> destination, @NonNull T marker);
    }

    private static @NonNull Map<String, Double> toMap(final @NonNull Point point) {
        return Map.of("x", point.x(), "z", point.z());
    }

}
