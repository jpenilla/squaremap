package xyz.jpenilla.squaremap.common.config;

/**
 * Limits concurrent image writers and images awaiting save completion for a world.
 * These limits apply to the same save queue regardless of the render mode.
 */
public final class ImageSavingConfig {
    public final int workers;
    public final int maxPendingImages;

    ImageSavingConfig(final AbstractWorldConfig<?> config) {
        this.workers = RenderConfig.fixed(config, "map.image-saving.workers", 2, 1);
        this.maxPendingImages = RenderConfig.fixed(config, "map.image-saving.max-pending-images", 100, 1);
    }
}
