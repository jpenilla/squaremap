package xyz.jpenilla.squaremap.common.visibilitylimit;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.common.util.Numbers;

@DefaultQualifier(NonNull.class)
final class PolygonShape implements VisibilityShape {
    private final List<Point> points;
    private final Bounds bounds;

    public PolygonShape(final List<Point> points) {
        this.points = points;
        this.bounds = calculateBounds(points);
    }

    @Override
    public boolean shouldRenderChunk(final MapWorld world, final int chunkX, final int chunkZ) {
        final int minX = Numbers.chunkToBlock(chunkX);
        final int minZ = Numbers.chunkToBlock(chunkZ);
        final int maxX = minX + 16;
        final int maxZ = minZ + 16;
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                if (this.contains(x, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean shouldRenderRegion(final MapWorld world, final int regionX, final int regionZ) {
        final int minX = Numbers.regionToChunk(regionX);
        final int minZ = Numbers.regionToChunk(regionZ);
        final int maxX = minX + 32;
        final int maxZ = minZ + 32;
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                if (this.shouldRenderChunk(world, x, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean shouldRenderColumn(final MapWorld world, final int blockX, final int blockZ) {
        return this.contains(blockX, blockZ);
    }

    @Override
    public int countChunksInRegion(final MapWorld world, final int regionX, final int regionZ) {
        final int minX = Numbers.regionToChunk(regionX);
        final int minZ = Numbers.regionToChunk(regionZ);
        final int maxX = minX + 32;
        final int maxZ = minZ + 32;
        int count = 0;
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                if (this.shouldRenderChunk(world, x, z)) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean contains(final double x, final double y) {
        final int npoints = this.points.size();

        if (npoints <= 2 || !this.bounds.contains(x, y)) {
            return false;
        }
        int hits = 0;

        int lastx = (int) this.points.get(npoints - 1).x();
        int lasty = (int) this.points.get(npoints - 1).z();
        int curx, cury;

        // Walk the edges of the polygon
        for (int i = 0; i < npoints; lastx = curx, lasty = cury, i++) {
            curx = (int) this.points.get(i).x();
            cury = (int) this.points.get(i).z();

            if (cury == lasty) {
                continue;
            }

            int leftx;
            if (curx < lastx) {
                if (x >= lastx) {
                    continue;
                }
                leftx = curx;
            } else {
                if (x >= curx) {
                    continue;
                }
                leftx = lastx;
            }

            double test1, test2;
            if (cury < lasty) {
                if (y < cury || y >= lasty) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - curx;
                test2 = y - cury;
            } else {
                if (y < lasty || y >= cury) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - lastx;
                test2 = y - lasty;
            }

            if (test1 < (test2 / (lasty - cury) * (lastx - curx))) {
                hits++;
            }
        }

        return ((hits & 1) != 0);
    }

    private static Bounds calculateBounds(final List<Point> points) {
        int boundsMinX = Integer.MAX_VALUE;
        int boundsMinY = Integer.MAX_VALUE;
        int boundsMaxX = Integer.MIN_VALUE;
        int boundsMaxY = Integer.MIN_VALUE;

        for (final Point p : points) {
            int x = (int) p.x();
            boundsMinX = Math.min(boundsMinX, x);
            boundsMaxX = Math.max(boundsMaxX, x);
            int y = (int) p.z();
            boundsMinY = Math.min(boundsMinY, y);
            boundsMaxY = Math.max(boundsMaxY, y);
        }
        return new Bounds(
            boundsMinX, boundsMinY,
            boundsMaxX - boundsMinX,
            boundsMaxY - boundsMinY
        );
    }

    private record Bounds(int x, int y, int width, int height) {
        public boolean contains(final double x, final double y) {
            double x0 = this.x;
            double y0 = this.y;
            return (x >= x0 &&
                y >= y0 &&
                x < x0 + this.width &&
                y < y0 + this.height);
        }
    }
}
