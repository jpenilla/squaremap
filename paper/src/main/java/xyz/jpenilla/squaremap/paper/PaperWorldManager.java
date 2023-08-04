package xyz.jpenilla.squaremap.paper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.WorldManagerImpl;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;
import xyz.jpenilla.squaremap.paper.util.WorldNameToKeyMigration;

@DefaultQualifier(NonNull.class)
@Singleton
public final class PaperWorldManager extends WorldManagerImpl {
    private final DirectoryProvider directoryProvider;

    @Inject
    private PaperWorldManager(
        final MapWorldInternal.Factory factory,
        final ServerAccess serverAccess,
        final DirectoryProvider directoryProvider,
        final ConfigManager configManager
    ) {
        super(factory, serverAccess, configManager);
        this.directoryProvider = directoryProvider;
    }

    @Override
    public void initWorld(final ServerLevel level) {
        WorldNameToKeyMigration.tryMoveDirectories(this.directoryProvider, level);
        super.initWorld(level);
    }

    public Optional<MapWorldInternal> getWorldIfEnabled(final World world) {
        return this.getWorldIfEnabled(CraftBukkitReflection.serverLevel(world));
    }
}
