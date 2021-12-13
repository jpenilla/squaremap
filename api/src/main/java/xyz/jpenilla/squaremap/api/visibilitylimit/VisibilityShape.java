package xyz.jpenilla.squaremap.api.visibilitylimit;

import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * @deprecated Unsupported API.
 */
@Deprecated(forRemoval = true)
public interface VisibilityShape {
    boolean shouldRenderChunk(@NonNull World world, int chunkX, int chunkZ);

    boolean shouldRenderRegion(@NonNull World world, int regionX, int regionZ);

    boolean shouldRenderColumn(@NonNull World world, int blockX, int blockZ);

    int countChunksInRegion(@NonNull World world, int regionX, int regionZ);
}
