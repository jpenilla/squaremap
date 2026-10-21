package xyz.jpenilla.squaremap.common.coordinate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public record TileCoordinate(int zoom, int x, int z) {
    /**
     * Gets the tile one zoom level out that contains this one.
     *
     * @return the parent tile
     */
    public TileCoordinate parent() {
        return new TileCoordinate(this.zoom - 1, Math.floorDiv(this.x, 2), Math.floorDiv(this.z, 2));
    }
}
