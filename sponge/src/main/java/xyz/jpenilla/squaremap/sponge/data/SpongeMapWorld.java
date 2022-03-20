package xyz.jpenilla.squaremap.sponge.data;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.time.Duration;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.Server;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.task.UpdateMarkers;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;

@DefaultQualifier(NonNull.class)
public final class SpongeMapWorld extends MapWorldInternal {
    private final ScheduledTask updateMarkers;

    @AssistedInject
    private SpongeMapWorld(
        final PluginContainer pluginContainer,
        @Assisted final ServerLevel level,
        final RenderFactory renderFactory,
        final DirectoryProvider directoryProvider
    ) {
        super(level, renderFactory, directoryProvider);

        this.updateMarkers = ((Server) level.getServer()).scheduler().submit(
            Task.builder()
                .plugin(pluginContainer)
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

    public interface Factory extends MapWorldInternal.Factory<SpongeMapWorld> {
        SpongeMapWorld create(ServerLevel level);
    }
}
