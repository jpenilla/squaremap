package xyz.jpenilla.squaremap.common.render.scanning;

import java.util.Map;
import java.util.concurrent.CancellationException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.chunksnapshot.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.coordinate.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.render.RenderControl;
import xyz.jpenilla.squaremap.common.render.RenderPlan;
import xyz.jpenilla.squaremap.common.render.output.RegionImage;
import xyz.jpenilla.squaremap.common.util.Colors;

/**
 * Computes map pixels from supplied snapshots, without loading chunks.
 * Instances must not be used concurrently.
 */
@DefaultQualifier(NonNull.class)
public final class ChunkScanner {
    private Map<ChunkCoordinate, @Nullable ChunkSnapshot> inputs = Map.of();
    private final BlockColors blockColors;
    private final ScanSettings settings;
    private final RenderControl control;
    private final @Nullable BiomeColorSampler biomes;
    private final BiomeSnapshotLookup biomeSnapshots = new BiomeSnapshotLookup();

    public ChunkScanner(
        final BlockColors blockColors,
        final BiomeColorTables biomeColorTables,
        final BiomeManager biomeManager,
        final BiomeResolver fallbackBiomes,
        final ScanSettings settings,
        final RenderControl control
    ) {
        this.blockColors = blockColors;
        this.settings = settings;
        this.control = control;
        this.biomes = settings.biomes() ? new BiomeColorSampler(biomeColorTables, biomeManager, fallbackBiomes, settings.biomeBlend(), this.biomeSnapshots) : null;
    }

    public void scan(final RenderPlan.ChunkWork work, final Map<ChunkCoordinate, @Nullable ChunkSnapshot> snapshots, final RegionImage image) {
        this.inputs = snapshots;
        try {
            if (this.biomes != null) {
                this.biomeSnapshots.begin(work.coordinate(), snapshots);
            }
            this.scanChunk(work, image);
        } finally {
            this.inputs = Map.of();
            this.biomeSnapshots.clear();
        }
    }

    private void scanChunk(final RenderPlan.ChunkWork work, final RegionImage image) {
        final ChunkCoordinate coordinate = work.coordinate();
        final @Nullable ChunkSnapshot target = this.inputs.get(coordinate);
        final int rows = work.topRowOnly() ? 1 : 16;
        if (target == null) {
            return;
        }
        final @Nullable ChunkSnapshot north = this.inputs.get(new ChunkCoordinate(coordinate.x(), coordinate.z() - 1));
        final int[] lastY = north == null ? new int[16] : this.bottomRowHeights(north);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < rows; z++) {
                this.checkCancelled();
                if (this.settings.visibility().shouldRenderColumn(coordinate.getBlockX() + x, coordinate.getBlockZ() + z)) {
                    final int color = this.scanBlock(target, x, z, lastY);
                    image.setPixel(coordinate.getBlockX() + x, coordinate.getBlockZ() + z, color);
                }
            }
        }
    }

    private void checkCancelled() {
        if (this.control.cancelled() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Render cancelled");
        }
    }

    private int effectiveMaxHeight(final ChunkSnapshot chunk) {
        return this.settings.maxHeight() == -1 ? chunk.getMaxY() + 1 : this.settings.maxHeight();
    }

    private int[] bottomRowHeights(final ChunkSnapshot chunk) {
        final int[] lastY = new int[16];
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            this.checkCancelled();
            final int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, 15) + 1;
            pos.set(chunk.pos().getMinBlockX() + x, Math.min(topY, this.effectiveMaxHeight(chunk)), chunk.pos().getMinBlockZ() + 15);
            final BlockState state = this.settings.iterateUp() ? this.iterateUp(chunk, pos) : this.iterateDown(chunk, pos);
            if (this.settings.clearGlass() && isGlass(state)) {
                this.handleGlass(chunk, pos);
            }
            lastY[x] = pos.getY();
        }
        return lastY;
    }

    private int scanBlock(final ChunkSnapshot chunk, final int imgX, final int imgZ, final int[] lastY) {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, imgX, imgZ) + 1;
        pos.set(chunk.pos().getMinBlockX() + imgX, Math.min(topY, this.effectiveMaxHeight(chunk)), chunk.pos().getMinBlockZ() + imgZ);
        if (topY <= chunk.getMinY()) {
            return Colors.clearMapColor();
        }
        BlockState state = this.settings.iterateUp() ? this.iterateUp(chunk, pos) : this.iterateDown(chunk, pos);
        if (this.settings.clearGlass() && isGlass(state)) {
            final int glassColor = this.blockColors.color(state);
            final float glassAlpha = state.getBlock() == Blocks.GLASS ? 0.25F : 0.5F;
            state = this.handleGlass(chunk, pos);
            return Colors.mix(this.color(chunk, imgX, imgZ, lastY, state, pos), glassColor, glassAlpha);
        }
        return this.color(chunk, imgX, imgZ, lastY, state, pos);
    }

    private int color(final ChunkSnapshot chunk, final int imgX, final int imgZ, final int[] lastY, final BlockState state, final BlockPos.MutableBlockPos pos) {
        int color = this.blockColors.color(state);
        if (this.biomes != null) {
            color = this.biomes.modifyColorFromBiome(color, chunk, pos);
        }
        final int odd = (imgX + imgZ & 1);
        final int curY = pos.getY();
        final int previousY = lastY[imgX];
        lastY[imgX] = curY;
        final @Nullable DepthResult depth = findDepthIfFluid(pos, state, chunk);
        if (depth != null) {
            return this.fluidColor(depth.depth, color, state, depth.state, odd);
        }
        final double diffY = (double) curY - previousY + ((double) odd - 0.5D) * 0.4D;
        final byte offset = (byte) (diffY > 0.6D ? 2 : (diffY < -0.6D ? 0 : 1));
        return Colors.shade(color, offset);
    }

    private BlockState iterateDown(final ChunkSnapshot chunk, final BlockPos.MutableBlockPos pos) {
        BlockState state;
        if (chunk.dimensionType().hasCeiling()) {
            do {
                pos.move(Direction.DOWN);
                state = chunk.getBlockState(pos);
            } while (!state.isAir() && pos.getY() > chunk.getMinY());
        }
        do {
            pos.move(Direction.DOWN);
            state = chunk.getBlockState(pos);
        } while ((this.blockColors.color(state) == Colors.clearMapColor() || this.settings.invisibleBlocks().contains(state.getBlock())) && pos.getY() > chunk.getMinY());
        return state;
    }

    private BlockState iterateUp(final ChunkSnapshot chunk, final BlockPos.MutableBlockPos pos) {
        BlockState state;
        final int height = pos.getY();
        pos.setY(chunk.getMinY());
        if (chunk.dimensionType().hasCeiling()) {
            do {
                pos.move(Direction.UP);
                state = chunk.getBlockState(pos);
            } while (!state.isAir() && pos.getY() < height);
            do {
                pos.move(Direction.UP);
                state = chunk.getBlockState(pos);
            } while (!this.settings.iterateUpBaseBlocks().contains(state.getBlock()) && pos.getY() < height);
        }
        do {
            pos.move(Direction.DOWN);
            state = chunk.getBlockState(pos);
        } while ((this.blockColors.color(state) == Colors.clearMapColor() || this.settings.invisibleBlocks().contains(state.getBlock())) && pos.getY() > chunk.getMinY());
        return state;
    }

    private BlockState handleGlass(final ChunkSnapshot chunk, final BlockPos.MutableBlockPos pos) {
        BlockState state = chunk.getBlockState(pos);
        while (isGlass(state)) {
            this.checkCancelled();
            state = this.iterateDown(chunk, pos);
        }
        return state;
    }

    private static boolean isGlass(final BlockState state) {
        return state.getBlock() == Blocks.GLASS || state.getBlock() instanceof StainedGlassBlock;
    }

    private static @Nullable DepthResult findDepthIfFluid(final BlockPos pos, final BlockState state, final ChunkSnapshot chunk) {
        if (pos.getY() <= chunk.getMinY() || state.getFluidState().isEmpty()) {
            return null;
        }
        final BlockPos.MutableBlockPos mutable = pos.mutable();
        BlockState under;
        int depth = 0;
        int y = pos.getY() - 1;
        do {
            mutable.setY(y--);
            under = chunk.getBlockState(mutable);
            ++depth;
        } while (y > chunk.getMinY() && depth <= 10 && !under.getFluidState().isEmpty());
        return new DepthResult(depth, under);
    }

    private int fluidColor(final int depth, int color, final BlockState fluidState, final BlockState under, final int odd) {
        final Fluid fluid = fluidTypeForRender(color, fluidState.getFluidState());
        boolean shaded = false;
        if (fluid == Fluids.WATER && this.settings.waterCheckerboard()) {
            color = depthCheckerboard(depth, color, odd);
            shaded = true;
        }
        if (fluid == Fluids.WATER && this.settings.clearWater()) {
            if (!this.settings.waterCheckerboard()) {
                color = Colors.shade(color, 0.85F - depth * 0.01F);
            }
            color = Colors.mix(color, this.blockColors.color(under), 0.20F / (depth / 2.0F));
            shaded = true;
        }
        if (fluid == Fluids.LAVA && this.settings.lavaCheckerboard()) {
            color = depthCheckerboard(depth, color, odd);
            shaded = true;
        }
        return shaded ? color : Colors.removeAlpha(color);
    }

    private static Fluid fluidTypeForRender(final int color, final FluidState state) {
        final Fluid fluid = state.getType();
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            return Fluids.WATER;
        }
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return Fluids.LAVA;
        }
        return (color >> 24 & 255) == 255 ? Fluids.LAVA : Fluids.WATER;
    }

    private static int depthCheckerboard(final double depth, final int color, final double odd) {
        final double diff = depth * 0.1D + odd * 0.2D;
        return Colors.shade(color, (byte) (diff < 0.5D ? 2 : (diff > 0.9D ? 0 : 1)));
    }

    private record DepthResult(int depth, BlockState state) {
    }

}
