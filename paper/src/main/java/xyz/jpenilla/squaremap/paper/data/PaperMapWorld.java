package xyz.jpenilla.squaremap.paper.data;

import net.minecraft.server.level.ServerLevel;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.task.UpdateMarkers;
import xyz.jpenilla.squaremap.paper.SquaremapPaper;
import xyz.jpenilla.squaremap.paper.util.BukkitRunnableAdapter;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

@DefaultQualifier(NonNull.class)
public final class PaperMapWorld extends MapWorldInternal {
    private final BukkitRunnable updateMarkersTask;

    public PaperMapWorld(final ServerLevel level) {
        super(level);

        this.updateMarkersTask = new BukkitRunnableAdapter(new UpdateMarkers(this));
        this.updateMarkersTask.runTaskTimer((SquaremapPaper) SquaremapCommon.instance().platform(), 20 * 5, 20L * this.config().MARKER_API_UPDATE_INTERVAL_SECONDS);
    }

    @Override
    public WorldIdentifier identifier() {
        return BukkitAdapter.worldIdentifier(this.bukkit());
    }

    public World bukkit() {
        return CraftBukkitReflection.world(this.serverLevel());
    }

    @Override
    public void shutdown() {
        this.updateMarkersTask.cancel();
        super.shutdown();
    }
}
