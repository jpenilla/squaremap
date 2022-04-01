package xyz.jpenilla.squaremap.paper.data;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.task.UpdateMarkers;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;
import xyz.jpenilla.squaremap.paper.SquaremapPaper;

@DefaultQualifier(NonNull.class)
public final class PaperMapWorld extends MapWorldInternal {
    private final BukkitTask updateMarkersTask;

    @AssistedInject
    private PaperMapWorld(
        final SquaremapPaper platform,
        @Assisted final ServerLevel level,
        final RenderFactory renderFactory,
        final DirectoryProvider directoryProvider,
        final Server server
    ) {
        super(level, renderFactory, directoryProvider);

        this.updateMarkersTask = server.getScheduler()
            .runTaskTimer(platform, new UpdateMarkers(this), 20 * 5, 20L * this.config().MARKER_API_UPDATE_INTERVAL_SECONDS);
    }

    @Override
    public void shutdown() {
        this.updateMarkersTask.cancel();
        super.shutdown();
    }

    public interface Factory extends MapWorldInternal.Factory<PaperMapWorld> {
        PaperMapWorld create(ServerLevel level);
    }
}
