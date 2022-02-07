package xyz.jpenilla.squaremap.common.visibilitylimit;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.MapWorld;

@DefaultQualifier(NonNull.class)
public interface VisibilityShape {
    boolean shouldRenderChunk(MapWorld world, int chunkX, int chunkZ);

    boolean shouldRenderRegion(MapWorld world, int regionX, int regionZ);

    boolean shouldRenderColumn(MapWorld world, int blockX, int blockZ);

    int countChunksInRegion(MapWorld world, int regionX, int regionZ);
}
