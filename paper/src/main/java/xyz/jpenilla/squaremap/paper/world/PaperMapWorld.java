package xyz.jpenilla.squaremap.paper.world;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapDirectories;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;
import xyz.jpenilla.squaremap.common.util.concurrent.ExceptionLoggingScheduledThreadPoolExecutor;
import xyz.jpenilla.squaremap.common.util.concurrent.Threads;
import xyz.jpenilla.squaremap.common.web.MarkerDataPublisher;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;
import xyz.jpenilla.squaremap.paper.util.Folia;

@DefaultQualifier(NonNull.class)
public final class PaperMapWorld extends MapWorldInternal {
    private final MarkerTaskHandler markerTaskHandler;

    @AssistedInject
    private PaperMapWorld(
        @Assisted final ServerLevel level,
        final JavaPlugin plugin,
        final RenderFactory renderFactory,
        final SquaremapDirectories directories,
        final Server server,
        final ConfigManager configManager,
        final MarkerDataPublisher.Factory taskFactory
    ) {
        super(level, renderFactory, directories, configManager);

        if (Folia.FOLIA) {
            this.markerTaskHandler = new FoliaMarkerTaskHandler(level, taskFactory);
        } else {
            this.markerTaskHandler = new PaperMarkerTaskHandler(plugin, server, taskFactory);
        }
    }

    @Override
    public BlockPos getSpawnPos() {
        return this.serverLevel().serverLevelData.getRespawnData().pos();
    }

    @Override
    public void shutdown() {
        this.markerTaskHandler.shutdown();
        super.shutdown();
    }

    private interface MarkerTaskHandler {
        void shutdown();
    }

    private final class PaperMarkerTaskHandler implements MarkerTaskHandler {
        private final BukkitTask updateMarkersTask;

        private PaperMarkerTaskHandler(
            final JavaPlugin plugin,
            final Server server,
            final MarkerDataPublisher.Factory taskFactory
        ) {
            this.updateMarkersTask = server.getScheduler()
                .runTaskTimer(plugin, taskFactory.create(PaperMapWorld.this), 20 * 5, 20L * PaperMapWorld.this.config().MARKER_API_UPDATE_INTERVAL_SECONDS);
        }

        @Override
        public void shutdown() {
            this.updateMarkersTask.cancel();
        }
    }

    private final class FoliaMarkerTaskHandler implements MarkerTaskHandler {
        private final ScheduledExecutorService markerThread;
        private final ScheduledFuture<?> updateMarkersTask;

        private FoliaMarkerTaskHandler(final ServerLevel level, final MarkerDataPublisher.Factory taskFactory) {
            this.markerThread = new ExceptionLoggingScheduledThreadPoolExecutor(1, Threads.squaremapThreadFactory("markers", level));
            this.updateMarkersTask = this.markerThread.scheduleAtFixedRate(
                taskFactory.create(PaperMapWorld.this),
                5,
                PaperMapWorld.this.config().MARKER_API_UPDATE_INTERVAL_SECONDS,
                TimeUnit.SECONDS
            );
        }

        @Override
        public void shutdown() {
            this.updateMarkersTask.cancel(false);
            Threads.shutdownExecutor(this.markerThread, TimeUnit.MILLISECONDS, 100);
        }
    }
}
