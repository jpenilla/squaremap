package xyz.jpenilla.squaremap.sponge.data;

import java.time.Duration;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.api.Server;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.task.UpdateMarkers;
import xyz.jpenilla.squaremap.sponge.SquaremapSponge;

public final class SpongeMapWorld extends MapWorldInternal {
    private final ScheduledTask updateMarkers;

    public SpongeMapWorld(final ServerLevel level) {
        super(level);
        this.updateMarkers = ((Server) level.getServer()).scheduler().submit(
            Task.builder()
                .plugin(((SquaremapSponge) SquaremapCommon.instance().platform()).pluginContainer())
                .delay(Duration.ofSeconds(5))
                .interval(Duration.ofSeconds(this.config().MARKER_API_UPDATE_INTERVAL_SECONDS))
                .execute(new Runnable() {
                    final UpdateMarkers updateMarkers = new UpdateMarkers(SpongeMapWorld.this);

                    @Override
                    public void run() {
                        this.updateMarkers.run();
                    }
                })
                .build()
        );
    }

    @Override
    public void shutdown() {
        this.updateMarkers.cancel();
        super.shutdown();
    }
}
