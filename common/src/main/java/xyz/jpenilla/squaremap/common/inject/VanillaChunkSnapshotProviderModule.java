package xyz.jpenilla.squaremap.common.inject;

import com.google.inject.AbstractModule;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.common.util.VanillaChunkSnapshotProvider;

@DefaultQualifier(NonNull.class)
public final class VanillaChunkSnapshotProviderModule extends AbstractModule {
    @Override
    protected void configure() {
        this.bind(ChunkSnapshotProvider.class)
            .to(VanillaChunkSnapshotProvider.class);
    }
}
