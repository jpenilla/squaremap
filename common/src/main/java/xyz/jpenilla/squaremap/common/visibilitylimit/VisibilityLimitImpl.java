package xyz.jpenilla.squaremap.common.visibilitylimit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.common.coordinate.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.coordinate.RegionCoordinate;

public final class VisibilityLimitImpl implements VisibilityLimit {
    private final List<VisibilityShape> shapes = new CopyOnWriteArrayList<>();
    private final MapWorld world;

    public VisibilityLimitImpl(MapWorld world) {
        this.world = world;
    }

    @Override
    public @NonNull List<VisibilityShape> getShapes() {
        return this.shapes;
    }

    @Override
    public boolean isWithinLimit(final int blockX, final int blockZ) {
        return this.shouldRenderColumn(blockX, blockZ);
    }

    public void load(final List<VisibilityShape> configLimits) {
        this.shapes.clear();
        for (final VisibilityShape shape : configLimits) {
            if (shape == VisibilityShape.NULL) {
                // enabled: false
                continue;
            }
            this.shapes.add(shape);
        }
    }

    /**
     * Copies the configured visibility shapes, replacing world-border shapes with their
     * current bounds so later border changes do not affect the render job's visibility checks.
     *
     * @return the snapshot
     */
    public VisibilityLimitImpl snapshot() {
        final VisibilityLimitImpl snapshot = new VisibilityLimitImpl(this.world);
        snapshot.load(this.shapes.stream()
            .map(shape -> shape instanceof WorldBorderShape border ? border.snapshot(this.world) : shape)
            .toList());
        return snapshot;
    }

    public boolean shouldRenderChunk(final ChunkCoordinate chunkCoord) {
        return this.shouldRenderChunk(chunkCoord.x(), chunkCoord.z());
    }

    public boolean shouldRenderChunk(final int chunkX, final int chunkZ) {
        if (this.shapes.size() == 0) {
            return true;
        }
        for (VisibilityShape shape : this.shapes) {
            if (shape.shouldRenderChunk(world, chunkX, chunkZ)) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldRenderColumn(final int blockX, final int blockZ) {
        if (this.shapes.size() == 0) {
            return true;
        }
        for (VisibilityShape shape : this.shapes) {
            if (shape.shouldRenderColumn(world, blockX, blockZ)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldRenderRegion(final int regionX, final int regionZ) {
        if (this.shapes.size() == 0) {
            return true;
        }
        for (VisibilityShape shape : this.shapes) {
            if (shape.shouldRenderRegion(world, regionX, regionZ)) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldRenderRegion(final RegionCoordinate region) {
        return this.shouldRenderRegion(region.x(), region.z());
    }
}
