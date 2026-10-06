package xyz.jpenilla.squaremap.fabric.world;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapDirectories;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;
import xyz.jpenilla.squaremap.common.web.MarkerDataPublisher;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;

@DefaultQualifier(NonNull.class)
public final class FabricMapWorld extends MapWorldInternal {
    private final MarkerDataPublisher updateMarkers;

    @AssistedInject
    private FabricMapWorld(
        @Assisted final ServerLevel level,
        final RenderFactory renderFactory,
        final SquaremapDirectories directories,
        final ConfigManager configManager,
        final MarkerDataPublisher.Factory taskFactory
    ) {
        super(level, renderFactory, directories, configManager);

        this.updateMarkers = taskFactory.create(this);
    }

    public void tickEachSecond(final long tick) {
        if (tick % (this.config().MARKER_API_UPDATE_INTERVAL_SECONDS * 20L) == 0) {
            this.updateMarkers.run();
        }
    }
}
