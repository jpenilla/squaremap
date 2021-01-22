package net.pl3x.map.api;

import net.pl3x.map.api.marker.Marker;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;

/**
 * Provides Markers and other metadata which make up a layer. LayerProviders are called on each update of a layer.
 */
public interface LayerProvider {

    /**
     * Get the label of this LayerProvider, shown in the control box
     *
     * @return label
     */
    @NonNull String getLabel();

    /**
     * Whether or not to show this layer in the control box
     *
     * <p>Default implementation always returns {@code true}</p>
     *
     * @return boolean
     */
    default boolean showControls() {
        return true;
    }

    /**
     * Whether this layer is hidden by default in the control box
     *
     * <p>Default implementation always returns {@code false}</p>
     *
     * @return boolean
     */
    default boolean defaultHidden() {
        return false;
    }

    /**
     * 0-indexed order for this layer in the control box
     * <p>Falls back to alpha-numeric ordering based on name if there are order conflicts</p>
     *
     * @return arbitrary number
     */
    int layerPriority();

    /**
     * 0-indexed z-index for this layer. Used in determining what layers are visually on top of other layers.
     *
     * <p>Falls back to alpha-numeric ordering based on name if there are order conflicts</p>
     *
     * <p>Default implementation returns {@link #layerPriority()}</p>
     *
     * @return arbitrary number
     */
    default int zIndex() {
        return this.layerPriority();
    }

    /**
     * Get the markers to display
     *
     * @return markers
     */
    @NonNull Collection<Marker> getMarkers();

    /**
     * Updates the timestamp this layer was last modified
     */
    default void updateTimestamp() {
    }

    /**
     * Gets the time since this layer was last modified
     *
     * @return last modified time (epoch millis)
     */
    default long getTimestamp() {
        return 0;
    }

}
