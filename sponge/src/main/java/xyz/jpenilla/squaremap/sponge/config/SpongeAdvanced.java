package xyz.jpenilla.squaremap.sponge.config;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Advanced;

@SuppressWarnings("unused")
public final class SpongeAdvanced {
    private SpongeAdvanced() {
    }

    public static final String CHUNK_GENERATION = "chunk-generation";
    public static final String CHUNK_LOAD = "chunk-load";

    public static final String BLOCK_PLACE = "block-place";
    public static final String BLOCK_BREAK = "block-break";
    public static final String BLOCK_MODIFY = "block-modify";
    public static final String BLOCK_GROWTH = "block-growth";
    public static final String BLOCK_DECAY = "block-decay";

    public static final String LIQUID_SPREAD = "liquid-spread";
    public static final String LIQUID_DECAY = "liquid-decay";

    private static final Object2BooleanMap<String> UPDATE_TRIGGERS = new Object2BooleanOpenHashMap<>();

    public static boolean listenerEnabled(final @NonNull String key) {
        if (!UPDATE_TRIGGERS.containsKey(key)) {
            Logging.logger().warn(String.format("No configuration option found for update trigger: %s, it will not be enabled.", key));
            return false;
        }
        return UPDATE_TRIGGERS.getBoolean(key);
    }

    private static void listenerToggles() {
        UPDATE_TRIGGERS.clear();

        final Set<String> defaultOn = Set.of(
            BLOCK_PLACE,
            BLOCK_BREAK,
            BLOCK_MODIFY,
            BLOCK_GROWTH,
            BLOCK_DECAY,
            CHUNK_GENERATION,
            LIQUID_SPREAD,
            LIQUID_DECAY
        );
        for (final String name : defaultOn) {
            UPDATE_TRIGGERS.put(name, Advanced.config().getBoolean("settings.map-update-triggers." + name, true));
        }

        final Set<String> defaultOff = Set.of(
            CHUNK_LOAD
        );
        for (final String name : defaultOff) {
            UPDATE_TRIGGERS.put(name, Advanced.config().getBoolean("settings.map-update-triggers." + name, false));
        }
    }
}
