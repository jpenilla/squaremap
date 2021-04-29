package net.pl3x.map.plugin.visibilitylimit;

import net.pl3x.map.api.visibilitylimit.VisibilityShape;
import net.pl3x.map.plugin.data.ChunkCoordinate;
import net.pl3x.map.plugin.data.Region;
import net.pl3x.map.plugin.util.Numbers;

import org.bukkit.World;
import org.bukkit.util.BlockVector;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Decides what blocks, chunks and regions are visible on the map. Even if a
 * chunk exists outside of this limit, it is not displayed.
 *
 */
public final class VisibilityLimit implements net.pl3x.map.api.visibilitylimit.VisibilityLimit {

    private static final int REGION_SIZE_CHUNKS = Numbers.regionToChunk(1);
    private final List<VisibilityShape> shapes = new CopyOnWriteArrayList<>();
    private final World world;

    public VisibilityLimit(@NonNull World world) {
        this.world = world;
    }

    /**
     * Counts the amount of chunks in the region for which
     * {@link #shouldRenderChunk(int, int)} returns {@code true}.
     * 
     * @param region
     *            The region.
     * @return The amount of chunks, from 0 to {@value #REGION_SIZE_CHUNKS} *
     *         {@value #REGION_SIZE_CHUNKS}.
     */
    public int countChunksInRegion(Region region) {
        switch (this.shapes.size()) {
            case 0:
                return REGION_SIZE_CHUNKS * REGION_SIZE_CHUNKS;
            case 1:
                return this.shapes.get(0).countChunksInRegion(world, region.getX(), region.getZ());
            default:
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
                return count;
        }
    }
    
    @Override
    public @NonNull List<VisibilityShape> getShapes() {
        return this.shapes;
    }

    @Override
    public boolean isWithinLimit(int blockX, int blockZ) {
        return this.shouldRenderColumn(blockX, blockZ);
    }

    public void parse(List<Map<String, Object>> configLimits) {
        this.shapes.clear();
        for (Map<String, Object> visibilityLimit : configLimits) {
            Object type = visibilityLimit.get("type");
            if (type == null) {
                continue;
            }
            if (type.equals("circle")) {
                parseCircleShape(visibilityLimit);
            } else if (type.equals("rectangle")) {
                parseRectangleShape(visibilityLimit);
            } else if (type.equals("world-border")) {
                parseWorldBorderShape(visibilityLimit);
            }
        }
    }

    private void parseCircleShape(Map<String, Object> visibilityLimit) {
        if (visibilityLimit.get("center-x") instanceof Number
                && visibilityLimit.get("center-z") instanceof Number
                && visibilityLimit.get("radius") instanceof Number) {
            int centerX = ((Number) visibilityLimit.get("center-x")).intValue();
            int centerZ = ((Number) visibilityLimit.get("center-z")).intValue();
            int radius = ((Number) visibilityLimit.get("radius")).intValue();
            if (radius > 0) {
                this.shapes.add(new CircleShape(centerX, centerZ, radius));
            }
        }
    }

    private void parseRectangleShape(Map<String, Object> visibilityLimit) {
        if (visibilityLimit.get("min-x") instanceof Number
                && visibilityLimit.get("min-z") instanceof Number
                && visibilityLimit.get("max-x") instanceof Number
                && visibilityLimit.get("max-z") instanceof Number) {
            int minX = ((Number) visibilityLimit.get("min-x")).intValue();
            int minZ = ((Number) visibilityLimit.get("min-z")).intValue();
            int maxX = ((Number) visibilityLimit.get("max-x")).intValue();
            int maxZ = ((Number) visibilityLimit.get("max-z")).intValue();
            if (maxX >= minX && maxZ >= minZ) {
                this.shapes.add(new RectangleShape(new BlockVector(minX, 0, minZ), new BlockVector(maxX, 0, maxZ)));
            }
        }
    }

    private void parseWorldBorderShape(Map<String, Object> visibilityLimit) {
        Object enabled = visibilityLimit.get("enabled");
        if (enabled != null && enabled.equals(Boolean.TRUE)) {
            this.shapes.add(new WorldBorderShape());
        }
    }

    public boolean shouldRenderChunk(ChunkCoordinate chunkCoord) {
        return this.shouldRenderChunk(chunkCoord.getX(), chunkCoord.getZ());
    }

    public boolean shouldRenderChunk(int chunkX, int chunkZ) {
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

    public boolean shouldRenderColumn(int blockX, int blockZ) {
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

    private boolean shouldRenderRegion(int regionX, int regionZ) {
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

    public boolean shouldRenderRegion(Region region) {
        return shouldRenderRegion(region.getX(), region.getZ());
    }



}
