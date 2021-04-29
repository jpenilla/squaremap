package net.pl3x.map.plugin.visibilitylimit;

import net.pl3x.map.api.visibilitylimit.VisibilityShape;
import net.pl3x.map.plugin.util.Numbers;

import org.bukkit.World;
import org.bukkit.util.BlockVector;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Limits map drawing to a rectangular region.
 *
 */
final class RectangleShape implements VisibilityShape {

    private final int minBlockX;
    private final int maxBlockX;
    private final int minBlockZ;
    private final int maxBlockZ;

    private final int minChunkX;
    private final int maxChunkX;
    private final int minChunkZ;
    private final int maxChunkZ;

    private final int minRegionX;
    private final int maxRegionX;
    private final int minRegionZ;
    private final int maxRegionZ;

    /**
     * Sets the minimum and maximum locations displayed on the map.
     * 
     * @param min
     *            Minimum location, inclusive.
     * @param max
     *            Maximum location, also inclusive.
     * @throws IllegalArgumentException
     *             If min > max.
     */
    RectangleShape(@NonNull BlockVector min, @NonNull BlockVector max) throws IllegalArgumentException {
        if (min.getBlockX() > max.getBlockX() || min.getBlockZ() > max.getBlockZ()) {
            throw new IllegalArgumentException("min > max: min=" + min + " max=" + max);
        }

        this.minBlockX = min.getBlockX();
        this.maxBlockX = max.getBlockX();
        this.minBlockZ = min.getBlockZ();
        this.maxBlockZ = max.getBlockZ();

        this.minChunkX = Numbers.blockToChunk(this.minBlockX);
        this.maxChunkX = Numbers.blockToChunk(this.maxBlockX);
        this.minChunkZ = Numbers.blockToChunk(this.minBlockZ);
        this.maxChunkZ = Numbers.blockToChunk(this.maxBlockZ);

        this.minRegionX = Numbers.blockToRegion(this.minBlockX);
        this.maxRegionX = Numbers.blockToRegion(this.maxBlockX);
        this.minRegionZ = Numbers.blockToRegion(this.minBlockZ);
        this.maxRegionZ = Numbers.blockToRegion(this.maxBlockZ);
    }

    @Override
    public boolean shouldRenderChunk(World world, int chunkX, int chunkZ) {
        return chunkX >= this.minChunkX && chunkX <= this.maxChunkX
                && chunkZ >= this.minChunkZ && chunkZ <= this.maxChunkZ;
    }

    @Override
    public boolean shouldRenderRegion(World world, int regionX, int regionZ) {
        return regionX >= this.minRegionX && regionX <= this.maxRegionX
                && regionZ >= this.minRegionZ && regionZ <= this.maxRegionZ;
    }

    @Override
    public boolean shouldRenderColumn(World world, int blockX, int blockZ) {
        return blockX >= this.minBlockX && blockX <= this.maxBlockX
                && blockZ >= this.minBlockZ && blockZ <= this.maxBlockZ;
    }

    @Override
    public int countChunksInRegion(World world, int regionX, int regionZ) {
        int regionMinChunkX = Numbers.regionToChunk(regionX);
        int regionMaxChunkX = Numbers.regionToChunk(regionX + 1) - 1;
        int regionMinChunkZ = Numbers.regionToChunk(regionZ);
        int regionMaxChunkZ = Numbers.regionToChunk(regionZ + 1) - 1;

        int chunkWidth = Math.min(regionMaxChunkX, this.maxChunkX) - Math.max(regionMinChunkX, this.minChunkX) + 1;
        int chunkHeight = Math.min(regionMaxChunkZ, this.maxChunkZ) - Math.max(regionMinChunkZ, this.minChunkZ) + 1;
        if (chunkWidth < 0 || chunkHeight < 0) {
            return 0;
        }
        return chunkWidth * chunkHeight;
    }
}
