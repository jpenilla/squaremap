package net.pl3x.map.api.visibilitylimit;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

public interface VisibilityLimit {

    /**
     * Gets the shapes used to decide the visibility limit. If one
     * of the shapes says that an area can be drawn, it will be drawn.
     *
     * <p>If the list is empty, the entire world is drawn.
     *
     * <p>This map is mutable, so you can add and remove visibility shapes.
     * @return The shapes.
     */
    @NonNull List<VisibilityShape> getShapes();

    /**
     * Returns whether the given block is within any of the visibility limits (see {@link #getShapes()}).
     * If there are not visibility limits defined, this method always returns true.
     * @param blockX X coord of the block.
     * @param blockZ Z coord of the block.
     * @return True is the location is visible on the map, false otherwise.
     */
    boolean isWithinLimit(int blockX, int blockZ);
}
