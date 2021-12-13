package xyz.jpenilla.squaremap.plugin.api;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;
import xyz.jpenilla.squaremap.plugin.configuration.Lang;
import xyz.jpenilla.squaremap.plugin.configuration.WorldConfig;
import xyz.jpenilla.squaremap.plugin.data.MapWorld;

public class WorldBorderProvider implements LayerProvider {
    public static final Key WORLDBORDER_KEY = Key.of("squaremap-worldborder");

    private final String label;
    private final boolean showControls;
    private final boolean defaultHidden;
    private final int layerPriority;
    private final int zIndex;
    private final World world;
    private final MarkerOptions options;

    public WorldBorderProvider(final @NonNull MapWorld world) {
        this.world = world.bukkit();
        final WorldConfig config = world.config();
        this.label = Lang.UI_WORLDBORDER_MARKER_LABEL;
        this.showControls = config.WORLDBORDER_MARKER_SHOW_CONTROLS;
        this.defaultHidden = config.WORLDBORDER_MARKER_DEFAULT_HIDDEN;
        this.layerPriority = config.WORLDBORDER_MARKER_LAYER_PRIORITY;
        this.zIndex = config.WORLDBORDER_MARKER_Z_INDEX;
        this.options = MarkerOptions.builder()
            .strokeColor(Color.RED)
            .strokeWeight(3)
            .hoverTooltip(this.label)
            .build();
    }

    @Override
    public @NonNull String getLabel() {
        return this.label;
    }

    @Override
    public boolean showControls() {
        return this.showControls;
    }

    @Override
    public boolean defaultHidden() {
        return this.defaultHidden;
    }

    @Override
    public int layerPriority() {
        return this.layerPriority;
    }

    @Override
    public int zIndex() {
        return this.zIndex;
    }

    @Override
    public @NonNull Collection<Marker> getMarkers() {
        WorldBorder border = this.world.getWorldBorder();
        Location center = border.getCenter();
        int x = center.getBlockX();
        int z = center.getBlockZ();
        double radius = border.getSize() / 2;
        return List.of(
            Marker.polyline(
                Point.of(x - radius, z - radius),
                Point.of(x + radius, z - radius),
                Point.of(x + radius, z + radius),
                Point.of(x - radius, z + radius),
                Point.of(x - radius, z - radius)
            ).markerOptions(this.options)
        );
    }
}
