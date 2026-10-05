package xyz.jpenilla.squaremap.common.config;

import xyz.jpenilla.squaremap.common.Logging;

public final class BackgroundRenderConfig extends RenderConfig {
    public final boolean enabled;
    public final int intervalSeconds;
    public final int maxChunksPerInterval;

    BackgroundRenderConfig(final AbstractWorldConfig<?> config) {
        super(config, "map.render.background", Math.max(1, automaticWorkers() * 2 / 3));
        final String path = "map.render.background";
        final String enabled = override(path + ".enabled", Boolean.toString(config.getBoolean(path + ".enabled", true)));
        if (!enabled.equalsIgnoreCase("true") && !enabled.equalsIgnoreCase("false")) {
            Logging.logger().warn("{}.enabled must be true or false; ignoring JVM override '{}'", path, enabled);
            this.enabled = config.getBoolean(path + ".enabled", true);
        } else {
            this.enabled = Boolean.parseBoolean(enabled);
        }
        this.intervalSeconds = fixed(config, path + ".interval-seconds", 15, 1);
        this.maxChunksPerInterval = fixed(config, path + ".max-chunks-per-interval", 2048, 1);
    }
}
