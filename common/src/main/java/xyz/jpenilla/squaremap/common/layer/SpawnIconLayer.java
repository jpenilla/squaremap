package xyz.jpenilla.squaremap.common.layer;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;

import static xyz.jpenilla.squaremap.api.Point.point;

@DefaultQualifier(NonNull.class)
public final class SpawnIconLayer implements LayerProvider {
    public static final Key KEY = Key.of("squaremap-spawn_icon");

    private final String label;
    private final boolean showControls;
    private final boolean defaultHidden;
    private final int layerPriority;
    private final int zIndex;
    private final MapWorldInternal world;
    private final MarkerOptions options;

    public SpawnIconLayer(final MapWorldInternal world) {
        this.world = world;
        final WorldConfig config = world.config();
        this.label = Messages.UI_SPAWN_MARKER_ICON_LABEL;
        this.showControls = config.SPAWN_MARKER_ICON_SHOW_CONTROLS;
        this.defaultHidden = config.SPAWN_MARKER_ICON_DEFAULT_HIDDEN;
        this.layerPriority = config.SPAWN_MARKER_ICON_LAYER_PRIORITY;
        this.zIndex = config.SPAWN_MARKER_ICON_Z_INDEX;
        this.options = MarkerOptions.builder().hoverTooltip(this.label).build();
    }

    @Override
    public String getLabel() {
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
    public Collection<Marker> getMarkers() {
        final BlockPos spawn = this.world.serverLevel().getSharedSpawnPos();
        return List.of(
            Marker.icon(point(spawn.getX(), spawn.getZ()), KEY, 16)
                .markerOptions(this.options)
        );
    }
}
