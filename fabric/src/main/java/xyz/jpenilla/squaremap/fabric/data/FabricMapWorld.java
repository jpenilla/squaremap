package xyz.jpenilla.squaremap.fabric.data;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.task.UpdateMarkers;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;

@DefaultQualifier(NonNull.class)
public final class FabricMapWorld extends MapWorldInternal {
    private final UpdateMarkers updateMarkers;

    @AssistedInject
    private FabricMapWorld(
        final SquaremapPlatform platform,
        @Assisted final ServerLevel level,
        final RenderFactory renderFactory,
        final DirectoryProvider directoryProvider
        ) {
        super(platform, level, renderFactory, directoryProvider);

        this.updateMarkers = new UpdateMarkers(this);
    }

    public void tickEachSecond(final long tick) {
        if (tick % (this.config().MARKER_API_UPDATE_INTERVAL_SECONDS * 20L) == 0) {
            this.updateMarkers.run();
        }
    }

    public interface Factory extends MapWorldInternal.Factory<FabricMapWorld> {
        FabricMapWorld create(ServerLevel level);
    }
}
