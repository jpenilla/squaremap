package xyz.jpenilla.squaremap.common.visibilitylimit;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.common.coordinate.CoordinateConversions;

/**
 * Limits map drawing to a circular region.
 */
@DefaultQualifier(NonNull.class)
final class CircleShape implements VisibilityShape {
    private static final int REGION_SIZE_BLOCKS = CoordinateConversions.regionToBlock(1);
    private static final int CHUNK_SIZE_BLOCKS = CoordinateConversions.chunkToBlock(1);

    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final int radiusSquared;

    public CircleShape(final int centerX, final int centerZ, final int radius) {
        if (radius < 1) {
            throw new IllegalArgumentException("Radius must be positive, but was " + radius);
        }
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.radiusSquared = radius * radius;
    }

    @Override
    public boolean shouldRenderChunk(final MapWorld world, final int chunkX, final int chunkZ) {
        if (this.radius == 0) {
            return false;
        }

        long blockX = CoordinateConversions.chunkToBlock(chunkX);
        long blockZ = CoordinateConversions.chunkToBlock(chunkZ);

        // make sure we look at the corner of the chunk that is the closest to the
        // center of our visibility limit
        if (blockX < this.centerX) {
            blockX += Math.min(CHUNK_SIZE_BLOCKS - 1, this.centerX - blockX);
        }
        if (blockZ < this.centerZ) {
            blockZ += Math.min(CHUNK_SIZE_BLOCKS - 1, this.centerZ - blockZ);
        }

        long distanceSquared = (blockX - this.centerX) * (blockX - this.centerX) + (blockZ - this.centerZ) * (blockZ - this.centerZ);
        return distanceSquared <= this.radiusSquared;

    }

    @Override
    public boolean shouldRenderRegion(final MapWorld world, final int regionX, final int regionZ) {
        if (this.radius == 0) {
            return false;
        }

        long blockX = CoordinateConversions.regionToBlock(regionX);
        long blockZ = CoordinateConversions.regionToBlock(regionZ);

        // make sure we look at the corner of the region that is the closest to the
        // center of our visibility limit
        if (blockX < this.centerX) {
            blockX += Math.min(REGION_SIZE_BLOCKS - 1, this.centerX - blockX);
        }
        if (blockZ < this.centerZ) {
            blockZ += Math.min(REGION_SIZE_BLOCKS - 1, this.centerZ - blockZ);
        }

        long distanceSquared = (blockX - this.centerX) * (blockX - this.centerX) + (blockZ - this.centerZ) * (blockZ - this.centerZ);
        return distanceSquared <= this.radiusSquared;
    }

    @Override
    public boolean shouldRenderColumn(final MapWorld world, final int blockX, final int blockZ) {
        if (this.radius == 0) {
            return false;
        }

        long distanceSquared = (long) (blockX - this.centerX) * (blockX - this.centerX) + (long) (blockZ - this.centerZ) * (blockZ - this.centerZ);
        return distanceSquared <= this.radiusSquared;
    }
}
