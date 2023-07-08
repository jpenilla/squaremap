package xyz.jpenilla.squaremap.paper.util.chunksnapshot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshotProviderFactory;

@DefaultQualifier(NonNull.class)
@Singleton
public final class PaperChunkSnapshotProviderFactory implements ChunkSnapshotProviderFactory {
    private final Server server;
    private final JavaPlugin plugin;

    @Inject
    private PaperChunkSnapshotProviderFactory(final Server server, final JavaPlugin plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public ChunkSnapshotProvider createChunkSnapshotProvider(final ServerLevel level) {
        return new PaperChunkSnapshotProvider(level, this.server, this.plugin);
    }
}
