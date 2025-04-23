package xyz.jpenilla.squaremap.sponge.data;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.time.Duration;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.Game;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.task.TaskFactory;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;

@DefaultQualifier(NonNull.class)
public final class SpongeMapWorld extends MapWorldInternal {
    private final ScheduledTask updateMarkers;

    @AssistedInject
    private SpongeMapWorld(
        @Assisted final ServerLevel level,
        final RenderFactory renderFactory,
        final DirectoryProvider directoryProvider,
        final Game game,
        final PluginContainer pluginContainer,
        final ConfigManager configManager,
        final TaskFactory taskFactory
    ) {
        super(level, renderFactory, directoryProvider, configManager);

        this.updateMarkers = game.server().scheduler().submit(
            Task.builder()
                .plugin(pluginContainer)
                .delay(Duration.ofSeconds(5))
                .interval(Duration.ofSeconds(this.config().MARKER_API_UPDATE_INTERVAL_SECONDS))
                .execute(taskFactory.createUpdateMarkers(this))
                .build()
        );
    }

    @Override
    public void shutdown() {
        this.updateMarkers.cancel();
        super.shutdown();
    }
}
