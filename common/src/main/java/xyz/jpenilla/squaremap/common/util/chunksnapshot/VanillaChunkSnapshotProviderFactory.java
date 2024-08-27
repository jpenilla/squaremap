package xyz.jpenilla.squaremap.common.util.chunksnapshot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;

@DefaultQualifier(NonNull.class)
@Singleton
public final class VanillaChunkSnapshotProviderFactory implements ChunkSnapshotProviderFactory {
    private final SquaremapPlatform platform;

    @Inject
    private VanillaChunkSnapshotProviderFactory(final SquaremapPlatform platform) {
        this.platform = platform;
    }

    @Override
    public ChunkSnapshotProvider createChunkSnapshotProvider(final ServerLevel level) {
        return new VanillaChunkSnapshotProvider(level, this.platform.hasMod("moonrise"));
    }
}
