package net.pl3x.map.plugin.visibilitylimit;

import net.pl3x.map.api.visibilitylimit.VisibilityShape;
import net.pl3x.map.plugin.util.Numbers;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * A visibility limit that follows the world border.
 */
@DefaultQualifier(NonNull.class)
public final class WorldBorderShape implements VisibilityShape {

    @Override
    public boolean shouldRenderChunk(final World world, final int chunkX, final int chunkZ) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        int radius = (int) Math.ceil(border.getSize() / 2);
        if (chunkX < Numbers.blockToChunk(center.getBlockX() - radius)
            || chunkX > Numbers.blockToChunk(center.getBlockX() + radius)) {
            return false;
        }
        if (chunkZ < Numbers.blockToChunk(center.getBlockZ() - radius)
            || chunkZ > Numbers.blockToChunk(center.getBlockZ() + radius)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldRenderRegion(final World world, final int regionX, final int regionZ) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        int radius = (int) Math.ceil(border.getSize() / 2);
        if (regionX < Numbers.blockToRegion(center.getBlockX() - radius)
            || regionX > Numbers.blockToRegion(center.getBlockX() + radius)) {
            return false;
        }
        if (regionZ < Numbers.blockToRegion(center.getBlockZ() - radius)
            || regionZ > Numbers.blockToRegion(center.getBlockZ() + radius)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldRenderColumn(final World world, final int blockX, final int blockZ) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        int radius = (int) Math.ceil(border.getSize() / 2);
        if (blockX < center.getBlockX() - radius || blockX >= center.getBlockX() + radius) {
            return false;
        }
        if (blockZ < center.getBlockZ() - radius || blockZ >= center.getBlockZ() + radius) {
            return false;
        }
        return true;
    }

    @Override
    public int countChunksInRegion(final World world, final int regionX, final int regionZ) {
        WorldBorder border = world.getWorldBorder();
        int regionMinChunkX = Numbers.regionToChunk(regionX);
        int regionMaxChunkX = Numbers.regionToChunk(regionX + 1) - 1;
        int regionMinChunkZ = Numbers.regionToChunk(regionZ);
        int regionMaxChunkZ = Numbers.regionToChunk(regionZ + 1) - 1;

        Location center = border.getCenter();
        int radius = (int) Math.ceil(border.getSize() / 2);
        int borderMinChunkX = Numbers.blockToChunk(center.getBlockX() - radius);
        int borderMaxChunkX = Numbers.blockToChunk(center.getBlockX() + radius);
        int borderMinChunkZ = Numbers.blockToChunk(center.getBlockZ() - radius);
        int borderMaxChunkZ = Numbers.blockToChunk(center.getBlockZ() + radius);

        int chunkWidth = Math.min(regionMaxChunkX, borderMaxChunkX) - Math.max(regionMinChunkX, borderMinChunkX) + 1;
        int chunkHeight = Math.min(regionMaxChunkZ, borderMaxChunkZ) - Math.max(regionMinChunkZ, borderMinChunkZ) + 1;
        if (chunkWidth < 0 || chunkHeight < 0) {
            return 0;
        }
        return chunkWidth * chunkHeight;
    }
}
