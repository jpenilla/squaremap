package xyz.jpenilla.squaremap.common.render.scanning;

import java.util.Set;
import net.minecraft.world.level.block.Block;
import xyz.jpenilla.squaremap.common.config.WorldAdvanced;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.visibilitylimit.VisibilityLimitImpl;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;

/**
 * Map appearance settings and visibility limits, captured when a render job is created.
 */
public record ScanSettings(
    boolean iterateUp,
    int maxHeight,
    boolean biomes,
    int biomeBlend,
    boolean clearGlass,
    boolean waterCheckerboard,
    boolean clearWater,
    boolean lavaCheckerboard,
    VisibilityLimitImpl visibility,
    Set<Block> invisibleBlocks,
    Set<Block> iterateUpBaseBlocks
) {
    /**
     * Blocks beyond a chunk whose biomes a scan may read: the blend radius plus the biome zoom margin.
     *
     * @return the biome margin
     */
    public int biomeMargin() {
        return this.biomeBlend + BiomeColorSampler.ZOOM_MARGIN;
    }

    public static ScanSettings capture(final MapWorldInternal world) {
        final WorldConfig config = world.config();
        final WorldAdvanced advanced = world.advanced();
        return new ScanSettings(
            config.MAP_ITERATE_UP, config.MAP_MAX_HEIGHT, config.MAP_BIOMES, config.MAP_BIOMES_BLEND,
            config.MAP_GLASS_CLEAR, config.MAP_WATER_CHECKERBOARD, config.MAP_WATER_CLEAR, config.MAP_LAVA_CHECKERBOARD,
            world.visibilityLimit().snapshot(),
            Set.copyOf(advanced.invisibleBlocks), Set.copyOf(advanced.iterateUpBaseBlocks)
        );
    }
}
