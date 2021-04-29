package net.pl3x.map.api.visibilitylimit;

import org.bukkit.World;

public interface VisibilityShape {
    boolean shouldRenderChunk(World world, int chunkX, int chunkZ);
    
    boolean shouldRenderRegion(World world, int regionX, int regionZ);
    
    boolean shouldRenderColumn(World world, int blockX, int blockZ);
    
    int countChunksInRegion(World world, int regionX, int regionZ);
}
