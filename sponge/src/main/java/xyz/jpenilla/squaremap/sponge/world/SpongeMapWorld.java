package xyz.jpenilla.squaremap.sponge.world;

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
import xyz.jpenilla.squaremap.common.SquaremapDirectories;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.render.RenderFactory;
import xyz.jpenilla.squaremap.common.web.MarkerDataPublisher;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;

@DefaultQualifier(NonNull.class)
public final class SpongeMapWorld extends MapWorldInternal {
    private final ScheduledTask updateMarkers;

    @AssistedInject
    private SpongeMapWorld(
        @Assisted final ServerLevel level,
        final RenderFactory renderFactory,
        final SquaremapDirectories directories,
        final Game game,
        final PluginContainer pluginContainer,
        final ConfigManager configManager,
        final MarkerDataPublisher.Factory taskFactory
    ) {
        super(level, renderFactory, directories, configManager);

        this.updateMarkers = game.server().scheduler().submit(
            Task.builder()
                .plugin(pluginContainer)
                .delay(Duration.ofSeconds(5))
                .interval(Duration.ofSeconds(this.config().MARKER_API_UPDATE_INTERVAL_SECONDS))
                .execute(taskFactory.create(this))
                .build()
        );
    }

    @Override
    public void shutdown() {
        this.updateMarkers.cancel();
        super.shutdown();
    }
}
