package xyz.jpenilla.squaremap.plugin.visibilitylimit;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.util.BlockVector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.api.visibilitylimit.VisibilityShape;
import xyz.jpenilla.squaremap.plugin.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.plugin.data.MapWorld;
import xyz.jpenilla.squaremap.plugin.data.RegionCoordinate;
import xyz.jpenilla.squaremap.plugin.util.Numbers;

public final class VisibilityLimit implements xyz.jpenilla.squaremap.api.visibilitylimit.VisibilityLimit {
    private static final int REGION_SIZE_CHUNKS = Numbers.regionToChunk(1);
    private final List<VisibilityShape> shapes = new CopyOnWriteArrayList<>();
    private final MapWorld world;

    public VisibilityLimit(MapWorld world) {
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
        switch (this.shapes.size()) {
            case 0:
                return REGION_SIZE_CHUNKS * REGION_SIZE_CHUNKS;
            case 1:
                return this.shapes.get(0).countChunksInRegion(world, region.x(), region.z());
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
    public boolean isWithinLimit(final int blockX, final int blockZ) {
        return this.shouldRenderColumn(blockX, blockZ);
    }

    public void parse(final List<Map<String, String>> configLimits) {
        this.shapes.clear();
        for (Map<String, String> visibilityLimit : configLimits) {
            String type = visibilityLimit.get("type");
            if (type == null) {
                continue;
            }
            switch (type) {
                case "circle" -> this.parseCircleShape(visibilityLimit);
                case "rectangle" -> this.parseRectangleShape(visibilityLimit);
                case "world-border" -> this.parseWorldBorderShape(visibilityLimit);
            }
        }
    }

    private static @Nullable Integer tryParseInt(final String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void parseCircleShape(final Map<String, String> visibilityLimit) {
        Integer centerX = tryParseInt(visibilityLimit.get("center-x"));
        Integer centerZ = tryParseInt(visibilityLimit.get("center-z"));
        Integer radius = tryParseInt(visibilityLimit.get("radius"));
        if (centerX != null && centerZ != null && radius != null) {
            if (radius > 0) {
                this.shapes.add(new CircleShape(centerX, centerZ, radius));
            }
        }
    }

    private void parseRectangleShape(final Map<String, String> visibilityLimit) {
        Integer minX = tryParseInt(visibilityLimit.get("min-x"));
        Integer minZ = tryParseInt(visibilityLimit.get("min-z"));
        Integer maxX = tryParseInt(visibilityLimit.get("max-x"));
        Integer maxZ = tryParseInt(visibilityLimit.get("max-z"));
        if (minX != null && minZ != null && maxX != null && maxZ != null) {
            if (maxX >= minX && maxZ >= minZ) {
                this.shapes.add(new RectangleShape(new BlockVector(minX, 0, minZ), new BlockVector(maxX, 0, maxZ)));
            }
        }
    }

    private void parseWorldBorderShape(final Map<String, String> visibilityLimit) {
        Object enabled = visibilityLimit.get("enabled");
        if (enabled != null && enabled.equals(Boolean.TRUE)) {
            this.shapes.add(new WorldBorderShape());
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
