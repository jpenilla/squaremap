package xyz.jpenilla.squaremap.paper;

import com.google.inject.Inject;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractWorldManager;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.paper.data.PaperMapWorld;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;
import xyz.jpenilla.squaremap.paper.util.WorldNameToKeyMigration;

@DefaultQualifier(NonNull.class)
public final class PaperWorldManager extends AbstractWorldManager<PaperMapWorld> {
    private final DirectoryProvider directoryProvider;

    @Inject
    private PaperWorldManager(
        final PaperMapWorld.Factory factory,
        final ServerAccess serverAccess,
        final DirectoryProvider directoryProvider
    ) {
        super(factory, serverAccess);
        this.directoryProvider = directoryProvider;
    }

    public Optional<PaperMapWorld> getWorldIfEnabled(final World world) {
        return this.getWorldIfEnabled(CraftBukkitReflection.serverLevel(world))
            .map(w -> (PaperMapWorld) w);
    }

    public void worldUnloaded(final World world) {
        this.worldUnloaded(CraftBukkitReflection.serverLevel(world));
    }

    @Override
    public void start() {
        for (final ServerLevel level : this.serverAccess.levels()) {
            WorldNameToKeyMigration.tryMoveDirectories(this.directoryProvider, level);
        }
        super.start();
    }
}
