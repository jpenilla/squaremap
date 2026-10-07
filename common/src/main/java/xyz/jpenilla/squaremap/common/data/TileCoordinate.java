package xyz.jpenilla.squaremap.common.data;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public record TileCoordinate(int zoom, int x, int z) {

    /**
     * Get the key identifying this tile in the tile update manifest. The format matches
     * the path of the tile image relative to the world tiles directory, without the file
     * extension, so that the web interface can derive it from Leaflet tile coordinates.
     *
     * @return the manifest key
     */
    public String key() {
        return this.zoom + "/" + this.x + "_" + this.z;
    }
}
