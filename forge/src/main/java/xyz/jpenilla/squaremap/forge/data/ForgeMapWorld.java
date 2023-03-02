package xyz.jpenilla.squaremap.forge.data;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.task.UpdateMarkers;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;

@DefaultQualifier(NonNull.class)
public final class ForgeMapWorld extends MapWorldInternal {
    private final UpdateMarkers updateMarkers;

    @AssistedInject
    private ForgeMapWorld(
        @Assisted final ServerLevel level,
        final RenderFactory renderFactory,
        final DirectoryProvider directoryProvider,
        final ConfigManager configManager
    ) {
        super(level, renderFactory, directoryProvider, configManager);

        this.updateMarkers = new UpdateMarkers(this);
    }

    public void tickEachSecond(final long tick) {
        if (tick % (this.config().MARKER_API_UPDATE_INTERVAL_SECONDS * 20L) == 0) {
            this.updateMarkers.run();
        }
    }

    public interface Factory extends MapWorldInternal.Factory<ForgeMapWorld> {
        @Override
        ForgeMapWorld create(ServerLevel level);
    }
}
