package xyz.jpenilla.squaremap.common.visibilitylimit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.Numbers;

public final class VisibilityLimitImpl implements VisibilityLimit {
    private static final int REGION_SIZE_CHUNKS = Numbers.regionToChunk(1);
    private final List<VisibilityShape> shapes = new CopyOnWriteArrayList<>();
    private final MapWorld world;

    public VisibilityLimitImpl(MapWorld world) {
        this.world = world;
    }

    /**
     * Counts the amount of chunks in the region for which
     * {@link #shouldRenderChunk(int, int)} returns {@code true}.
     *
     * @param region The region.
     * @return The amount of chunks, from 0 to {@link #REGION_SIZE_CHUNKS} *
     * {@link #REGION_SIZE_CHUNKS}.
     */
    public int countChunksInRegion(final @NonNull RegionCoordinate region) {
        return switch (this.shapes.size()) {
            case 0 -> REGION_SIZE_CHUNKS * REGION_SIZE_CHUNKS;
            case 1 -> this.shapes.get(0).countChunksInRegion(world, region.x(), region.z());
            default -> {
                // multiple shapes overlap - need to check each chunk individually

                int chunkXStart = region.getChunkX();
                int chunkZStart = region.getChunkZ();

                int count = 0;
                for (int i = 0; i < REGION_SIZE_CHUNKS; i++) {
                    for (int j = 0; j < REGION_SIZE_CHUNKS; j++) {
                        if (this.shouldRenderChunk(chunkXStart + i, chunkZStart + j)) {
                            count++;
                        }
                    }
                }
                yield count;
            }
        };
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
