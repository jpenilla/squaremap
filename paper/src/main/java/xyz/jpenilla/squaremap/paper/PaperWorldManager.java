package xyz.jpenilla.squaremap.paper;

import java.util.Optional;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.WorldManagerImpl;
import xyz.jpenilla.squaremap.paper.data.PaperMapWorld;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

@DefaultQualifier(NonNull.class)
public final class PaperWorldManager extends WorldManagerImpl<PaperMapWorld> {
    PaperWorldManager() {
        super(PaperMapWorld::new);
    }

    public Optional<PaperMapWorld> getWorldIfEnabled(final World world) {
        return this.getWorldIfEnabled(CraftBukkitReflection.serverLevel(world))
            .map(w -> (PaperMapWorld) w);
    }

    public void worldUnloaded(final World world) {
        this.worldUnloaded(CraftBukkitReflection.serverLevel(world));
    }
}
