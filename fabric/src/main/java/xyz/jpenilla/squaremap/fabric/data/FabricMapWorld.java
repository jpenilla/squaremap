package xyz.jpenilla.squaremap.fabric.data;

import net.minecraft.server.level.ServerLevel;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.task.UpdateMarkers;

public final class FabricMapWorld extends MapWorldInternal {
    private final UpdateMarkers updateMarkers;

    public FabricMapWorld(final ServerLevel level) {
        super(level);

        this.updateMarkers = new UpdateMarkers(this);
    }

    public void tickEachSecond(final long tick) {
        if (tick % (this.config().MARKER_API_UPDATE_INTERVAL_SECONDS * 20L) == 0) {
            this.updateMarkers.run();
        }
    }
}
