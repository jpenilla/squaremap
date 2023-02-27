package xyz.jpenilla.squaremap.common.util.chunksnapshot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
@Singleton
public final class VanillaChunkSnapshotProviderFactory implements ChunkSnapshotProviderFactory {
    @Inject
    private VanillaChunkSnapshotProviderFactory() {
    }

    @Override
    public ChunkSnapshotProvider createChunkSnapshotProvider(final ServerLevel level) {
        return new VanillaChunkSnapshotProvider(level);
    }
}
