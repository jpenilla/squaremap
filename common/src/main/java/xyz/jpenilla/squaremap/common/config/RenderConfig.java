package xyz.jpenilla.squaremap.common.config;

import xyz.jpenilla.squaremap.common.Logging;

/**
 * Execution limits for one render mode.
 */
public class RenderConfig {
    public final int workers;
    public final int maxChunkRequests;
    public final int maxActiveChunks;
    public final int maxActiveRegions;
    public final int maxColumnChunks;
    public final int idleChunkCacheSize;

    RenderConfig(final AbstractWorldConfig<?> config, final String path) {
        this(config, path, automaticWorkers());
    }

    RenderConfig(final AbstractWorldConfig<?> config, final String path, final int defaultWorkers) {
        this.workers = automatic(config, path + ".workers", defaultWorkers, 1);
        this.maxChunkRequests = automatic(config, path + ".max-chunk-requests", Math.multiplyExact(this.workers, 96), 1);
        this.maxActiveChunks = automatic(config, path + ".max-active-chunks", Math.multiplyExact(this.workers, 64), 1);
        this.maxActiveRegions = fixed(config, path + ".max-active-regions", 2, 1);
        this.maxColumnChunks = fixed(config, path + ".max-column-chunks", 16, 1);
        this.idleChunkCacheSize = automatic(config, path + ".idle-chunk-cache-size", this.maxActiveChunks, 0);
    }

    private static int automatic(final AbstractWorldConfig<?> config, final String path, final int fallback, final int minimum) {
        return parse(override(path, config.getString(path, "default")), path, fallback, minimum, true);
    }

    static int fixed(final AbstractWorldConfig<?> config, final String path, final int fallback, final int minimum) {
        return parse(override(path, Integer.toString(config.getInt(path, fallback))), path, fallback, minimum, false);
    }

    static String override(final String path, final String configured) {
        return System.getProperty("squaremap." + path, configured);
    }

    static int parse(final String value, final String path, final int fallback, final int minimum, final boolean automatic) {
        if (automatic && value.equalsIgnoreCase("default")) {
            return fallback;
        }
        final int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (final NumberFormatException ex) {
            Logging.logger().warn("{} must be an integer{}; using {} instead of '{}'", path, automatic ? " or 'default'" : "", fallback, value);
            return fallback;
        }
        if (parsed < minimum) {
            Logging.logger().warn("{} must be at least {}; using {} instead of {}", path, minimum, fallback, parsed);
            return fallback;
        }
        return parsed;
    }

    static int automaticWorkers() {
        final int halfProcessors = Runtime.getRuntime().availableProcessors() / 2;
        if (halfProcessors <= 4) {
            return halfProcessors <= 3 ? 1 : 2;
        }
        return halfProcessors / 2;
    }
}
