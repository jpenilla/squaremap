package xyz.jpenilla.squaremap.common.coordinate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public record TileCoordinate(int zoom, int x, int z) {
}
