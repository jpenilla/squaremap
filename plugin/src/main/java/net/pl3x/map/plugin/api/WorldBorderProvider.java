package net.pl3x.map.plugin.api;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import net.pl3x.map.api.Key;
import net.pl3x.map.api.LayerProvider;
import net.pl3x.map.api.Point;
import net.pl3x.map.api.marker.Marker;
import net.pl3x.map.api.marker.MarkerOptions;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.configuration.WorldConfig;
import net.pl3x.map.plugin.data.MapWorld;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.checkerframework.checker.nullness.qual.NonNull;

public class WorldBorderProvider implements LayerProvider {

    public static final Key WORLDBORDER_KEY = Key.of("pl3xmap-worldborder");

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
        return Collections.singletonList(Marker
                .polyline(
                        Point.of(x - radius, z - radius),
                        Point.of(x + radius, z - radius),
                        Point.of(x + radius, z + radius),
                        Point.of(x - radius, z + radius),
                        Point.of(x - radius, z - radius)
                )
                .markerOptions(this.options)
        );
    }

}
