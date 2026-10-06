package xyz.jpenilla.squaremap.common.visibilitylimit;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.border.WorldBorder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.common.coordinate.CoordinateConversions;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;

/**
 * A visibility limit that follows the world border.
 */
@DefaultQualifier(NonNull.class)
public final class WorldBorderShape implements VisibilityShape {

    @Override
    public boolean shouldRenderChunk(final MapWorld world, final int chunkX, final int chunkZ) {
        final WorldBorder border = ((MapWorldInternal) world).serverLevel().getWorldBorder();
        final BlockPos center = new BlockPos((int) border.getCenterX(), 0, (int) border.getCenterZ());
        int radius = (int) Math.ceil(border.getSize() / 2);
        if (chunkX < CoordinateConversions.blockToChunk(center.getX() - radius)
            || chunkX > CoordinateConversions.blockToChunk(center.getX() + radius)) {
            return false;
        }
        if (chunkZ < CoordinateConversions.blockToChunk(center.getZ() - radius)
            || chunkZ > CoordinateConversions.blockToChunk(center.getZ() + radius)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldRenderRegion(final MapWorld world, final int regionX, final int regionZ) {
        final WorldBorder border = ((MapWorldInternal) world).serverLevel().getWorldBorder();
        final BlockPos center = new BlockPos((int) border.getCenterX(), 0, (int) border.getCenterZ());
        int radius = (int) Math.ceil(border.getSize() / 2);
        if (regionX < CoordinateConversions.blockToRegion(center.getX() - radius)
            || regionX > CoordinateConversions.blockToRegion(center.getX() + radius)) {
            return false;
        }
        if (regionZ < CoordinateConversions.blockToRegion(center.getZ() - radius)
            || regionZ > CoordinateConversions.blockToRegion(center.getZ() + radius)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldRenderColumn(final MapWorld world, final int blockX, final int blockZ) {
        final WorldBorder border = ((MapWorldInternal) world).serverLevel().getWorldBorder();
        final BlockPos center = new BlockPos((int) border.getCenterX(), 0, (int) border.getCenterZ());
        int radius = (int) Math.ceil(border.getSize() / 2);
        if (blockX < center.getX() - radius || blockX >= center.getX() + radius) {
            return false;
        }
        if (blockZ < center.getZ() - radius || blockZ >= center.getZ() + radius) {
            return false;
        }
        return true;
    }

    @Override
    public int countChunksInRegion(final MapWorld world, final int regionX, final int regionZ) {
        final WorldBorder border = ((MapWorldInternal) world).serverLevel().getWorldBorder();
        int regionMinChunkX = CoordinateConversions.regionToChunk(regionX);
        int regionMaxChunkX = CoordinateConversions.regionToChunk(regionX + 1) - 1;
        int regionMinChunkZ = CoordinateConversions.regionToChunk(regionZ);
        int regionMaxChunkZ = CoordinateConversions.regionToChunk(regionZ + 1) - 1;

        final BlockPos center = new BlockPos((int) border.getCenterX(), 0, (int) border.getCenterZ());
        int radius = (int) Math.ceil(border.getSize() / 2);
        int borderMinChunkX = CoordinateConversions.blockToChunk(center.getX() - radius);
        int borderMaxChunkX = CoordinateConversions.blockToChunk(center.getX() + radius);
        int borderMinChunkZ = CoordinateConversions.blockToChunk(center.getZ() - radius);
        int borderMaxChunkZ = CoordinateConversions.blockToChunk(center.getZ() + radius);

        int chunkWidth = Math.min(regionMaxChunkX, borderMaxChunkX) - Math.max(regionMinChunkX, borderMinChunkX) + 1;
        int chunkHeight = Math.min(regionMaxChunkZ, borderMaxChunkZ) - Math.max(regionMinChunkZ, borderMinChunkZ) + 1;
        if (chunkWidth < 0 || chunkHeight < 0) {
            return 0;
        }
        return chunkWidth * chunkHeight;
    }
}
