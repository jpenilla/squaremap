package xyz.jpenilla.squaremap.api.visibilitylimit;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Decides what blocks, chunks and regions are visible on the map. A chunk outside this limit
 * will not be displayed regardless of whether it exists.
 *
 * @deprecated Unsupported API.
 */
@Deprecated(forRemoval = true)
public interface VisibilityLimit {

    /**
     * Gets the shapes used to decide the visibility limit. If one
     * of the shapes says that an area can be drawn, it will be drawn.
     *
     * <p>If the list is empty, the entire world is drawn.</p>
     *
     * <p>This map is mutable, so you can add and remove visibility shapes.</p>
     *
     * @return The shapes
     */
    @NonNull List<VisibilityShape> getShapes();

    /**
     * Returns whether the given block is within any of the visibility limits (see {@link #getShapes()}).
     * If there are no visibility limits defined, this method always returns true.
     *
     * @param blockX X coordinate of the block
     * @param blockZ Z coordinate of the block
     * @return whether the location is visible on the map
     */
    boolean isWithinLimit(int blockX, int blockZ);
}
