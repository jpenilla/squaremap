package xyz.jpenilla.squaremap.sponge.config;

import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.config.Advanced;

@SuppressWarnings("unused")
public final class SpongeAdvanced {
    private SpongeAdvanced() {
    }

    public static boolean CHUNK_GENERATION = true;
    public static boolean CHUNK_LOAD = false;

    public static boolean BLOCK_PLACE = true;
    public static boolean BLOCK_BREAK = true;
    public static boolean BLOCK_MODIFY = true;
    public static boolean BLOCK_GROWTH = true;
    public static boolean BLOCK_DECAY = true;

    public static boolean LIQUID_SPREAD = true;
    public static boolean LIQUID_DECAY = true;

    private static boolean listenerEnabled(final @NonNull String key, final boolean def) {
        return Advanced.config().getBoolean("settings.map-update-triggers." + key, def);
    }

    private static void listenerToggles() {
        CHUNK_GENERATION = listenerEnabled("chunk-generation", CHUNK_GENERATION);
        CHUNK_LOAD = listenerEnabled("chunk-load", CHUNK_LOAD);

        BLOCK_PLACE = listenerEnabled("block-place", BLOCK_PLACE);
        BLOCK_BREAK = listenerEnabled("block-break", BLOCK_BREAK);
        BLOCK_MODIFY = listenerEnabled("block-modify", BLOCK_MODIFY);
        BLOCK_GROWTH = listenerEnabled("block-growth", BLOCK_GROWTH);
        BLOCK_DECAY = listenerEnabled("block-decay", BLOCK_DECAY);

        LIQUID_SPREAD = listenerEnabled("liquid-spread", LIQUID_SPREAD);
        LIQUID_DECAY = listenerEnabled("liquid-decay", LIQUID_DECAY);
    }
}
