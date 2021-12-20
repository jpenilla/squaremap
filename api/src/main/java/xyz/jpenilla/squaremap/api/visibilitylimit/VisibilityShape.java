package xyz.jpenilla.squaremap.api.visibilitylimit;

import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.MapWorld;

/**
 * @deprecated Unsupported API.
 */
@Deprecated
public interface VisibilityShape {
    boolean shouldRenderChunk(@NonNull MapWorld world, int chunkX, int chunkZ);

    boolean shouldRenderRegion(@NonNull MapWorld world, int regionX, int regionZ);

    boolean shouldRenderColumn(@NonNull MapWorld world, int blockX, int blockZ);

    int countChunksInRegion(@NonNull MapWorld world, int regionX, int regionZ);
}
