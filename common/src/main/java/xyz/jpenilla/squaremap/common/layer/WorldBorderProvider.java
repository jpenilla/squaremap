package xyz.jpenilla.squaremap.common.layer;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import net.minecraft.world.level.border.WorldBorder;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;

public class WorldBorderProvider implements LayerProvider {
    public static final Key WORLDBORDER_KEY = Key.of("squaremap-worldborder");

    private final String label;
    private final boolean showControls;
    private final boolean defaultHidden;
    private final int layerPriority;
    private final int zIndex;
    private final MarkerOptions options;
    private final MapWorldInternal world;

    public WorldBorderProvider(final @NonNull MapWorldInternal world) {
        this.world = world;
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
        final WorldBorder border = this.world.serverLevel().getWorldBorder();
        final int x = (int) border.getCenterX();
        final int z = (int) border.getCenterZ();
        final double radius = border.getSize() / 2;
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
