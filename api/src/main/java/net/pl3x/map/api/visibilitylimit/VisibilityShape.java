package net.pl3x.map.api.visibilitylimit;

import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface VisibilityShape {
    boolean shouldRenderChunk(@NonNull World world, int chunkX, int chunkZ);

    boolean shouldRenderRegion(@NonNull World world, int regionX, int regionZ);

    boolean shouldRenderColumn(@NonNull World world, int blockX, int blockZ);

    int countChunksInRegion(@NonNull World world, int regionX, int regionZ);
}
