package xyz.jpenilla.squaremap.common.visibilitylimit;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.border.WorldBorder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.Numbers;

/**
 * A visibility limit that follows the world border.
 */
@DefaultQualifier(NonNull.class)
public final class WorldBorderShape implements VisibilityShape {

    @Override
    public boolean shouldRenderChunk(final MapWorld world, final int chunkX, final int chunkZ) {
        final WorldBorder border = ((MapWorldInternal) world).serverLevel().getWorldBorder();
        final BlockPos center = new BlockPos(border.getCenterX(), 0, border.getCenterZ());
        int radius = (int) Math.ceil(border.getSize() / 2);
        if (chunkX < Numbers.blockToChunk(center.getX() - radius)
            || chunkX > Numbers.blockToChunk(center.getX() + radius)) {
            return false;
        }
        if (chunkZ < Numbers.blockToChunk(center.getZ() - radius)
            || chunkZ > Numbers.blockToChunk(center.getZ() + radius)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldRenderRegion(final MapWorld world, final int regionX, final int regionZ) {
        final WorldBorder border = ((MapWorldInternal) world).serverLevel().getWorldBorder();
        final BlockPos center = new BlockPos(border.getCenterX(), 0, border.getCenterZ());
        int radius = (int) Math.ceil(border.getSize() / 2);
        if (regionX < Numbers.blockToRegion(center.getX() - radius)
            || regionX > Numbers.blockToRegion(center.getX() + radius)) {
            return false;
        }
        if (regionZ < Numbers.blockToRegion(center.getZ() - radius)
            || regionZ > Numbers.blockToRegion(center.getZ() + radius)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldRenderColumn(final MapWorld world, final int blockX, final int blockZ) {
        final WorldBorder border = ((MapWorldInternal) world).serverLevel().getWorldBorder();
        final BlockPos center = new BlockPos(border.getCenterX(), 0, border.getCenterZ());
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
        int regionMinChunkX = Numbers.regionToChunk(regionX);
        int regionMaxChunkX = Numbers.regionToChunk(regionX + 1) - 1;
        int regionMinChunkZ = Numbers.regionToChunk(regionZ);
        int regionMaxChunkZ = Numbers.regionToChunk(regionZ + 1) - 1;

        final BlockPos center = new BlockPos(border.getCenterX(), 0, border.getCenterZ());
        int radius = (int) Math.ceil(border.getSize() / 2);
        int borderMinChunkX = Numbers.blockToChunk(center.getX() - radius);
        int borderMaxChunkX = Numbers.blockToChunk(center.getX() + radius);
        int borderMinChunkZ = Numbers.blockToChunk(center.getZ() - radius);
        int borderMaxChunkZ = Numbers.blockToChunk(center.getZ() + radius);

        int chunkWidth = Math.min(regionMaxChunkX, borderMaxChunkX) - Math.max(regionMinChunkX, borderMinChunkX) + 1;
        int chunkHeight = Math.min(regionMaxChunkZ, borderMaxChunkZ) - Math.max(regionMinChunkZ, borderMinChunkZ) + 1;
        if (chunkWidth < 0 || chunkHeight < 0) {
            return 0;
        }
        return chunkWidth * chunkHeight;
    }
}
