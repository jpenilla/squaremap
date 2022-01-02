package xyz.jpenilla.squaremap.common.util;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface ChunkSnapshotProvider {
    CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(
        final ServerLevel level,
        final int x,
        final int z,
        final boolean biomesOnly
    );
}
