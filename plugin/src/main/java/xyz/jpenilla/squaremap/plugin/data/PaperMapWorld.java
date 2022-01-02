package xyz.jpenilla.squaremap.plugin.data;

import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.task.UpdateMarkers;
import xyz.jpenilla.squaremap.plugin.util.CraftBukkitReflection;

public final class PaperMapWorld extends MapWorldInternal {
    private final UpdateMarkers updateMarkersTask;

    private PaperMapWorld(final @NonNull World world) {
        super(CraftBukkitReflection.serverLevel(world));

        this.updateMarkersTask = new UpdateMarkers(this);
        this.updateMarkersTask.runTaskTimer(SquaremapPlugin.getInstance(), 20 * 5, 20L * this.config().MARKER_API_UPDATE_INTERVAL_SECONDS);
    }

    public static @NonNull PaperMapWorld forWorld(final @NonNull World world) {
        return new PaperMapWorld(world);
    }

    @Override
    public @NonNull String name() {
        return this.bukkit().getName();
    }

    @Override
    public @NonNull WorldIdentifier identifier() {
        return BukkitAdapter.worldIdentifier(this.bukkit());
    }

    public @NonNull World bukkit() {
        return this.serverLevel().getWorld();
    }

    @Override
    public void shutdown() {
        this.updateMarkersTask.cancel();
        super.shutdown();
    }
}
