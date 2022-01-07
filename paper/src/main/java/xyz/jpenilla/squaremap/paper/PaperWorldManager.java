package xyz.jpenilla.squaremap.paper;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.WorldManagerImpl;
import xyz.jpenilla.squaremap.paper.data.PaperMapWorld;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

public final class PaperWorldManager extends WorldManagerImpl<PaperMapWorld> {
    PaperWorldManager() {
        super(PaperMapWorld::new);
    }

    public @NonNull Optional<PaperMapWorld> getWorldIfEnabled(final @NonNull World world) {
        return this.getWorldIfEnabled(CraftBukkitReflection.serverLevel(world))
            .map(w -> (PaperMapWorld) w);
    }

    public void start() {
        Bukkit.getWorlds().forEach(this::getWorldIfEnabled);
    }

    public void worldUnloaded(final @NonNull World world) {
        this.worldUnloaded(CraftBukkitReflection.serverLevel(world));
    }
}
