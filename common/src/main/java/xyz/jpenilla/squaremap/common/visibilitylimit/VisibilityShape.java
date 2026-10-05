package xyz.jpenilla.squaremap.common.visibilitylimit;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.MapWorld;

@DefaultQualifier(NonNull.class)
public interface VisibilityShape {
    VisibilityShape NULL = new VisibilityShape() {
        @Override
        public boolean shouldRenderChunk(final MapWorld world, final int chunkX, final int chunkZ) {
            return true;
        }

        @Override
        public boolean shouldRenderRegion(final MapWorld world, final int regionX, final int regionZ) {
            return true;
        }

        @Override
        public boolean shouldRenderColumn(final MapWorld world, final int blockX, final int blockZ) {
            return true;
        }
    };

    boolean shouldRenderChunk(MapWorld world, int chunkX, int chunkZ);

    boolean shouldRenderRegion(MapWorld world, int regionX, int regionZ);

    boolean shouldRenderColumn(MapWorld world, int blockX, int blockZ);
}
