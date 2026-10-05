package xyz.jpenilla.squaremap.common.visibilitylimit;

import net.minecraft.world.level.border.WorldBorder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.common.coordinate.CoordinateConversions;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;

/**
 * A visibility limit that follows the world border.
 */
@DefaultQualifier(NonNull.class)
public final class WorldBorderShape implements VisibilityShape {
    private final @Nullable Bounds captured;

    public WorldBorderShape() {
        this.captured = null;
    }

    private WorldBorderShape(final Bounds captured) {
        this.captured = captured;
    }

    WorldBorderShape snapshot(final MapWorld world) {
        return new WorldBorderShape(this.bounds(world));
    }

    private Bounds bounds(final MapWorld world) {
        if (this.captured != null) {
            return this.captured;
        }
        final WorldBorder border = ((MapWorldInternal) world).serverLevel().getWorldBorder();
        final int centerX = (int) border.getCenterX();
        final int centerZ = (int) border.getCenterZ();
        final int radius = (int) Math.ceil(border.getSize() / 2);
        return new Bounds(centerX - radius, centerX + radius, centerZ - radius, centerZ + radius);
    }

    @Override
    public boolean shouldRenderChunk(final MapWorld world, final int chunkX, final int chunkZ) {
        final Bounds bounds = this.bounds(world);
        return chunkX >= CoordinateConversions.blockToChunk(bounds.minX) && chunkX <= CoordinateConversions.blockToChunk(bounds.maxX)
            && chunkZ >= CoordinateConversions.blockToChunk(bounds.minZ) && chunkZ <= CoordinateConversions.blockToChunk(bounds.maxZ);
    }

    @Override
    public boolean shouldRenderRegion(final MapWorld world, final int regionX, final int regionZ) {
        final Bounds bounds = this.bounds(world);
        return regionX >= CoordinateConversions.blockToRegion(bounds.minX) && regionX <= CoordinateConversions.blockToRegion(bounds.maxX)
            && regionZ >= CoordinateConversions.blockToRegion(bounds.minZ) && regionZ <= CoordinateConversions.blockToRegion(bounds.maxZ);
    }

    @Override
    public boolean shouldRenderColumn(final MapWorld world, final int blockX, final int blockZ) {
        final Bounds bounds = this.bounds(world);
        return blockX >= bounds.minX && blockX < bounds.maxX
            && blockZ >= bounds.minZ && blockZ < bounds.maxZ;
    }

    private record Bounds(int minX, int maxX, int minZ, int maxZ) {
    }
}
