package xyz.jpenilla.squaremap.common.inject.module;

import com.google.inject.AbstractModule;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshotProviderFactory;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.VanillaChunkSnapshotProviderFactory;

@DefaultQualifier(NonNull.class)
public final class VanillaChunkSnapshotProviderFactoryModule extends AbstractModule {
    @Override
    protected void configure() {
        this.bind(ChunkSnapshotProviderFactory.class)
            .to(VanillaChunkSnapshotProviderFactory.class);
    }
}
