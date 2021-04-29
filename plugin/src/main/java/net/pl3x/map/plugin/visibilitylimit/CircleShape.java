package net.pl3x.map.plugin.visibilitylimit;

import net.pl3x.map.api.visibilitylimit.VisibilityShape;
import net.pl3x.map.plugin.util.Numbers;

import org.bukkit.World;

/**
 * Limits map drawing to a circular region.
 *
 */
final class CircleShape implements VisibilityShape {

    private static final int REGION_SIZE_BLOCKS = Numbers.regionToBlock(1);
    private static final int CHUNK_SIZE_BLOCKS = Numbers.chunkToBlock(1);
    private static final int REGION_SIZE_CHUNKS = Numbers.regionToChunk(1);

    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final int radiusSquared;

    public CircleShape(int centerX, int centerZ, int radius) {
        if (radius < 1) {
            throw new IllegalArgumentException("Radius must be positive, but was " + radius);
        }
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.radiusSquared = radius * radius;
    }

    @Override
    public boolean shouldRenderChunk(World world, int chunkX, int chunkZ) {
        if (radius == 0) {
            return false;
        }

        long blockX = Numbers.chunkToBlock(chunkX);
        long blockZ = Numbers.chunkToBlock(chunkZ);

        // make sure we look at the corner of the chunk that is the closest to the
        // center of our visibility limit
        if (blockX < centerX) {
            blockX += Math.min(CHUNK_SIZE_BLOCKS - 1, centerX - blockX);
        }
        if (blockZ < centerZ) {
            blockZ += Math.min(CHUNK_SIZE_BLOCKS - 1, centerZ - blockZ);
        }

        long distanceSquared = (blockX - centerX) * (blockX - centerX) + (blockZ - centerZ) * (blockZ - centerZ);
        return distanceSquared <= radiusSquared;

    }

    @Override
    public boolean shouldRenderRegion(World world, int regionX, int regionZ) {
        if (radius == 0) {
            return false;
        }

        long blockX = Numbers.regionToBlock(regionX);
        long blockZ = Numbers.regionToBlock(regionZ);

        // make sure we look at the corner of the region that is the closest to the
        // center of our visibility limit
        if (blockX < centerX) {
            blockX += Math.min(REGION_SIZE_BLOCKS - 1, centerX - blockX);
        }
        if (blockZ < centerZ) {
            blockZ += Math.min(REGION_SIZE_BLOCKS - 1, centerZ - blockZ);
        }

        long distanceSquared = (blockX - centerX) * (blockX - centerX) + (blockZ - centerZ) * (blockZ - centerZ);
        return distanceSquared <= radiusSquared;
    }

    @Override
    public boolean shouldRenderColumn(World world, int blockX, int blockZ) {
        if (radius == 0) {
            return false;
        }

        long distanceSquared = (blockX - centerX) * (blockX - centerX) + (blockZ - centerZ) * (blockZ - centerZ);
        return distanceSquared <= radiusSquared;
    }

    @Override
    public int countChunksInRegion(World world, int regionX, int regionZ) {
        int chunkXStart = Numbers.regionToChunk(regionX);
        int chunkZStart = Numbers.regionToChunk(regionZ);
        if (shouldRenderChunk(world, chunkXStart, chunkZStart)
                && shouldRenderChunk(world, chunkXStart + REGION_SIZE_CHUNKS - 1, chunkZStart)
                && shouldRenderChunk(world, chunkXStart, chunkZStart + REGION_SIZE_CHUNKS - 1)
                && shouldRenderChunk(world, chunkXStart + REGION_SIZE_CHUNKS - 1, chunkZStart + REGION_SIZE_CHUNKS - 1)) {
            // we need to render all four corners, so that means we need to render the
            // entire region
            // (note: this only works because the visibility limit is one single circle)
            return REGION_SIZE_CHUNKS * REGION_SIZE_CHUNKS;
        }

        // check each chunk individually
        int count = 0;
        for (int i = 0; i < REGION_SIZE_CHUNKS; i++) {
            for (int j = 0; j < REGION_SIZE_CHUNKS; j++) {
                if (this.shouldRenderChunk(world, chunkXStart + i, chunkZStart + j)) {
                    count++;
                }
            }
        }
        return count;

    }

}
