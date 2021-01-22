package net.pl3x.map.plugin.api;

import net.pl3x.map.api.Key;
import net.pl3x.map.api.LayerProvider;
import net.pl3x.map.api.Point;
import net.pl3x.map.api.marker.Marker;
import net.pl3x.map.api.marker.MarkerOptions;
import net.pl3x.map.plugin.configuration.WorldConfig;
import net.pl3x.map.plugin.data.MapWorld;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Collections;

public class SpawnIconProvider implements LayerProvider {

    private final String label;
    private final boolean showControls;
    private final boolean defaultHidden;
    private final int layerPriority;
    private final int zIndex;
    private final World world;
    private final Key key;
    private final MarkerOptions options;

    public SpawnIconProvider(final @NonNull MapWorld world, final @NonNull Key key) {
        this.world = world.bukkit();
        this.key = key;
        final WorldConfig config = world.config();
        this.label = config.SPAWN_MARKER_ICON_LABEL;
        this.showControls = config.SPAWN_MARKER_ICON_SHOW_CONTROLS;
        this.defaultHidden = config.SPAWN_MARKER_ICON_DEFAULT_HIDDEN;
        this.layerPriority = config.SPAWN_MARKER_ICON_LAYER_PRIORITY;
        this.zIndex = config.SPAWN_MARKER_ICON_Z_INDEX;
        this.options = MarkerOptions.builder().hoverTooltip(this.label).build();
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
        return Collections.singletonList(Marker.icon(
                Point.fromLocation(this.world.getSpawnLocation()),
                this.key,
                16
        ).markerOptions(this.options));
    }

}
