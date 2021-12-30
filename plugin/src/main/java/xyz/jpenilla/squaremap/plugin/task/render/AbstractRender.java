package xyz.jpenilla.squaremap.plugin.task.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.minimessage.Template;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.apache.logging.log4j.LogManager;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.api.Pair;
import xyz.jpenilla.squaremap.plugin.Logging;
import xyz.jpenilla.squaremap.plugin.config.Lang;
import xyz.jpenilla.squaremap.plugin.data.BiomeColors;
import xyz.jpenilla.squaremap.plugin.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.plugin.data.Image;
import xyz.jpenilla.squaremap.plugin.data.MapWorld;
import xyz.jpenilla.squaremap.plugin.data.RegionCoordinate;
import xyz.jpenilla.squaremap.plugin.util.ChunkSnapshot;
import xyz.jpenilla.squaremap.plugin.util.Colors;
import xyz.jpenilla.squaremap.plugin.util.Numbers;
import xyz.jpenilla.squaremap.plugin.util.ReflectionUtil;
import xyz.jpenilla.squaremap.plugin.util.Util;

public abstract class AbstractRender implements Runnable {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();

    private final ExecutorService executor;
    private final FutureTask<Void> futureTask;
    protected volatile boolean cancelled = false;

    protected final MapWorld mapWorld;
    protected final World world;
    protected final ServerLevel level;

    private final ThreadLocal<BiomeColors> biomeColors;

    protected final AtomicInteger curChunks = new AtomicInteger(0);
    protected final AtomicInteger curRegions = new AtomicInteger(0);

    protected Pair<Timer, RenderProgress> progress = null;

    public AbstractRender(final @NonNull MapWorld world) {
        this(
            world,
            Executors.newFixedThreadPool(
                getThreads(world.config().MAX_RENDER_THREADS),
                Util.squaremapThreadFactory("render-worker", world.serverLevel())
            )
        );
    }

    public AbstractRender(final @NonNull MapWorld mapWorld, final @NonNull ExecutorService executor) {
        this.futureTask = new FutureTask<>(this, null);
        this.mapWorld = mapWorld;
        this.executor = executor;
        this.world = mapWorld.bukkit();
        this.level = ReflectionUtil.CraftBukkit.serverLevel(this.world);
        this.biomeColors = this.mapWorld.config().MAP_BIOMES
            ? ThreadLocal.withInitial(() -> new BiomeColors(mapWorld))
            : null; // this should be null if we are not mapping biomes
    }

    public static int getThreads(int threads) {
        if (threads == -1) {
            threads = Runtime.getRuntime().availableProcessors() / 3;
        }
        return Math.max(1, threads);
    }

    public synchronized void cancel() {
        if (this.progress != null) {
            this.progress.left().cancel();
        }
        this.cancelled = true;
        Util.shutdownExecutor(this.executor, TimeUnit.SECONDS, 1L);
        this.futureTask.cancel(false);
    }

    public void restartProgressLogger() {
        final RenderProgress old;
        if (this.progress != null) {
            this.progress.left().cancel();
            old = this.progress.right();
        } else {
            old = null;
        }
        this.progress = RenderProgress.printProgress(this, old);
    }

    @Override
    public final void run() {
        this.render();

        if (!(this instanceof BackgroundRender)) {
            final boolean finished = !this.cancelled;

            this.mapWorld.stopRender();

            if (finished) {
                this.mapWorld.finishedRender();
                Logging.info(Lang.LOG_FINISHED_RENDERING, Template.template("world", this.world.getName()));
            } else {
                Logging.info(Lang.LOG_CANCELLED_RENDERING, Template.template("world", this.world.getName()));
            }
        }
    }

    public abstract int totalChunks();

    public final int processedChunks() {
        return this.curChunks.get();
    }

    public abstract int totalRegions();

    public final int processedRegions() {
        return this.curRegions.get();
    }

    protected abstract void render();

    public final @NonNull FutureTask<Void> getFutureTask() {
        return this.futureTask;
    }

    protected final void mapRegion(final @NonNull RegionCoordinate region) {
        Image image = new Image(region, this.mapWorld.tilesPath(), this.mapWorld.config().ZOOM_MAX);
        int startX = region.getChunkX();
        int startZ = region.getChunkZ();
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int chunkX = startX; chunkX < startX + 32; chunkX++) {
            futures.add(this.mapChunkColumn(image, chunkX, startZ));
        }
        final List<Exception> exceptions = new ArrayList<>();
        for (final CompletableFuture<Void> future : futures) {
            try {
                future.join();
            } catch (final Exception ex) {
                exceptions.add(ex);
            }
        }
        for (final Exception exception : exceptions) {
            LOGGER.warn("Exception mapping region {}", region, exception);
        }
        if (!this.cancelled) {
            this.mapWorld.saveImage(image);
        }
    }

    protected final @NonNull CompletableFuture<Void> mapChunkColumn(final @NonNull Image image, final int chunkX, final int startChunkZ) {
        return CompletableFuture.runAsync(() -> {
            int[] lastY = new int[16];
            for (int chunkZ = startChunkZ; chunkZ < startChunkZ + 32; chunkZ++) {
                if (this.cancelled) return;
                if (!this.mapWorld.visibilityLimit().shouldRenderChunk(chunkX, chunkZ)) {
                    // skip rendering this chunk in the chunk column - it's outside the visibility limit
                    // (this chunk was already excluded from the chunk count, so not incrementing that is on purpose)
                    continue;
                }

                ChunkSnapshot chunk;
                if (chunkZ == startChunkZ) {
                    // this is the top line of the image, we need to
                    // scan the bottom line of the region to the north
                    // in order to get the correct lastY for shading
                    chunk = this.chunkSnapshot(this.level, chunkX, chunkZ - 1);
                    if (chunk != null) {
                        lastY = this.getLastYFromBottomRow(chunk);
                    }
                }
                chunk = this.chunkSnapshot(this.level, chunkX, chunkZ);
                if (chunk != null) {
                    this.scanChunk(image, lastY, chunk);
                }
                this.curChunks.incrementAndGet();
            }
        }, this.executor).exceptionally(thr -> {
            LOGGER.warn("mapChunkColumn failed!", thr);
            return null;
        });
    }

    protected final @NonNull CompletableFuture<Void> mapSingleChunk(final @NonNull Image image, final int chunkX, final int chunkZ) {
        return CompletableFuture.runAsync(() -> {
            int[] lastY = new int[16];

            ChunkSnapshot chunk;

            // try scanning south row of northern chunk to get proper yDiff
            chunk = this.chunkSnapshot(this.level, chunkX, chunkZ - 1);
            if (chunk != null) {
                lastY = this.getLastYFromBottomRow(chunk);
            }

            // scan the chunk itself
            chunk = this.chunkSnapshot(this.level, chunkX, chunkZ);
            if (chunk != null) {
                this.scanChunk(image, lastY, chunk);
            }

            // queue up the southern chunk in case it was stored with improper yDiff
            // https://github.com/pl3xgaming/Pl3xMap/issues/15
            final int down = chunkZ + 1;
            if (Numbers.chunkToRegion(chunkZ) == Numbers.chunkToRegion(down)) {
                chunk = this.chunkSnapshot(this.level, chunkX, down);
                if (chunk != null) {
                    this.scanTopRow(image, lastY, chunk);
                }
            } else {
                // chunk belongs to a different region, add to queue
                this.mapWorld.chunkModified(new ChunkCoordinate(chunkX, down));
            }

            this.curChunks.incrementAndGet();
        }, this.executor).exceptionally(thr -> {
            LOGGER.warn("mapSingleChunk failed!", thr);
            return null;
        });
    }

    private void scanChunk(Image image, int[] lastY, ChunkSnapshot chunk) {
        while (this.mapWorld.rendersPaused()) {
            sleep(500);
        }
        final int blockX = chunk.pos().getMinBlockX();
        final int blockZ = chunk.pos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (this.cancelled) {
                    return;
                }
                if (this.mapWorld.visibilityLimit().shouldRenderColumn(blockX + x, blockZ + z)) {
                    image.setPixel(blockX + x, blockZ + z, this.scanBlock(chunk, x, z, lastY));
                }
            }
        }
    }

    private void scanTopRow(Image image, int[] lastY, ChunkSnapshot chunk) {
        final int blockX = chunk.pos().getMinBlockX();
        final int blockZ = chunk.pos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            if (this.cancelled) {
                return;
            }
            if (this.mapWorld.visibilityLimit().shouldRenderColumn(blockX + x, blockZ)) {
                image.setPixel(blockX + x, blockZ, this.scanBlock(chunk, x, 0, lastY));
            }
        }
    }

    private int effectiveMaxHeight(final ChunkSnapshot chunk) {
        return this.mapWorld.config().MAP_MAX_HEIGHT == -1
            ? chunk.dimensionType().logicalHeight()
            : this.mapWorld.config().MAP_MAX_HEIGHT;
    }

    private int @NonNull [] getLastYFromBottomRow(final @NonNull ChunkSnapshot chunk) {
        final int[] lastY = new int[16];
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            if (this.cancelled) {
                return lastY;
            }
            final int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, 15) + 1;
            mutablePos.set(
                chunk.pos().getMinBlockX() + x,
                Math.min(topY, this.effectiveMaxHeight(chunk)),
                chunk.pos().getMinBlockZ() + 15
            );
            final BlockState state = this.mapWorld.config().MAP_ITERATE_UP
                ? this.iterateUp(chunk, mutablePos)
                : this.iterateDown(chunk, mutablePos);
            if (this.mapWorld.config().MAP_GLASS_CLEAR && isGlass(state)) {
                this.handleGlass(chunk, mutablePos);
            }
            lastY[x] = mutablePos.getY();
        }
        return lastY;
    }

    private int scanBlock(final ChunkSnapshot chunk, final int imgX, final int imgZ, final int[] lastY) {
        int blockX = chunk.pos().getMinBlockX() + imgX;
        int blockZ = chunk.pos().getMinBlockZ() + imgZ;

        BlockState state;
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        final int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, imgX, imgZ) + 1;
        mutablePos.set(blockX, Math.min(topY, this.effectiveMaxHeight(chunk)), blockZ);

        if (topY > chunk.getMinBuildHeight() + 1) {
            state = this.mapWorld.config().MAP_ITERATE_UP
                ? this.iterateUp(chunk, mutablePos)
                : this.iterateDown(chunk, mutablePos);
        } else {
            // no blocks found, show invisible/air
            return Colors.clearMapColor();
        }

        if (this.mapWorld.config().MAP_GLASS_CLEAR && isGlass(state)) {
            final int glassColor = this.mapWorld.getMapColor(state);
            final float glassAlpha = state.getBlock() == Blocks.GLASS ? 0.25F : 0.5F;
            state = this.handleGlass(chunk, mutablePos);
            final int color = this.getColor(chunk, imgX, imgZ, lastY, state, mutablePos);
            return Colors.mix(color, glassColor, glassAlpha);
        }

        return this.getColor(chunk, imgX, imgZ, lastY, state, mutablePos);
    }

    private int getColor(final @NonNull ChunkSnapshot chunk, final int imgX, final int imgZ, final int[] lastY, final @NonNull BlockState state, final BlockPos.@NonNull MutableBlockPos mutablePos) {
        int color = this.mapWorld.getMapColor(state);

        if (this.biomeColors != null) {
            color = this.biomeColors.get().modifyColorFromBiome(color, chunk, mutablePos);
        }

        int odd = (imgX + imgZ & 1);

        final @Nullable DepthResult fluidDepthResult = findDepthIfFluid(mutablePos, state, chunk);
        if (fluidDepthResult != null) {
            final int fluidDepth = fluidDepthResult.depth;
            final BlockState blockUnder = fluidDepthResult.state;
            return this.getFluidColor(fluidDepth, color, state, blockUnder, odd);
        }

        final int curY = mutablePos.getY();
        double diffY = ((double) curY - lastY[imgX]) * 4.0D / (double) 4 + ((double) odd - 0.5D) * 0.4D;
        byte colorOffset = (byte) (diffY > 0.6D ? 2 : (diffY < -0.6D ? 0 : 1));
        lastY[imgX] = curY;
        return Colors.shade(color, colorOffset);
    }

    private @NonNull BlockState iterateDown(final @NonNull ChunkSnapshot chunk, final BlockPos.@NonNull MutableBlockPos mutablePos) {
        BlockState state;
        if (chunk.dimensionType().hasCeiling()) {
            do {
                mutablePos.move(Direction.DOWN);
                state = chunk.getBlockState(mutablePos);
            } while (!state.isAir() && mutablePos.getY() > chunk.getMinBuildHeight());
        }
        do {
            mutablePos.move(Direction.DOWN);
            state = chunk.getBlockState(mutablePos);
        } while ((this.mapWorld.getMapColor(state) == Colors.clearMapColor() || this.mapWorld.advanced().invisibleBlocks.contains(state.getBlock())) && mutablePos.getY() > chunk.getMinBuildHeight());
        return state;
    }

    private @NonNull BlockState iterateUp(final @NonNull ChunkSnapshot chunk, final BlockPos.@NonNull MutableBlockPos mutablePos) {
        BlockState state;
        int height = mutablePos.getY();
        mutablePos.setY(chunk.getMinBuildHeight());
        if (chunk.dimensionType().hasCeiling()) {
            do {
                mutablePos.move(Direction.UP);
                state = chunk.getBlockState(mutablePos);
            } while (!state.isAir() && mutablePos.getY() < height);
            do {
                mutablePos.move(Direction.UP);
                state = chunk.getBlockState(mutablePos);
            } while (!this.mapWorld.advanced().iterateUpBaseBlocks.contains(state.getBlock()) && mutablePos.getY() < height);
        }
        do {
            mutablePos.move(Direction.DOWN);
            state = chunk.getBlockState(mutablePos);
        } while ((this.mapWorld.getMapColor(state) == Colors.clearMapColor() || this.mapWorld.advanced().invisibleBlocks.contains(state.getBlock())) && mutablePos.getY() > chunk.getMinBuildHeight());
        return state;
    }

    private static boolean isGlass(final @NonNull BlockState state) {
        final Block block = state.getBlock();
        return block == Blocks.GLASS || block instanceof StainedGlassBlock;
    }

    private @NonNull BlockState handleGlass(final @NonNull ChunkSnapshot chunk, final BlockPos.@NonNull MutableBlockPos mutablePos) {
        BlockState state = chunk.getBlockState(mutablePos);
        while (isGlass(state)) {
            state = this.iterateDown(chunk, mutablePos);
        }
        return state;
    }

    private record DepthResult(int depth, BlockState state) {
    }

    private static @Nullable DepthResult findDepthIfFluid(final @NonNull BlockPos blockPos, final @NonNull BlockState state, final @NonNull ChunkSnapshot chunk) {
        if (blockPos.getY() > chunk.getMinBuildHeight() && !state.getFluidState().isEmpty()) {
            BlockState fluidState;
            int fluidDepth = 0;

            int yBelowSurface = blockPos.getY() - 1;
            final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            mutablePos.set(blockPos);
            do {
                mutablePos.setY(yBelowSurface--);
                fluidState = chunk.getBlockState(mutablePos);
                ++fluidDepth;
            } while (yBelowSurface > chunk.getMinBuildHeight() && fluidDepth <= 10 && !fluidState.getFluidState().isEmpty());

            return new DepthResult(fluidDepth, fluidState);
        }
        return null;
    }

    private int getFluidColor(final int fluidCountY, int color, final @NonNull BlockState fluidState, final @NonNull BlockState underBlock, final int odd) {
        final Fluid fluid = fluidState.getFluidState().getType();
        boolean shaded = false;
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            if (this.mapWorld.config().MAP_WATER_CHECKERBOARD) {
                color = applyDepthCheckerboard(fluidCountY, color, odd);
                shaded = true;
            }
            if (this.mapWorld.config().MAP_WATER_CLEAR) {
                if (!this.mapWorld.config().MAP_WATER_CHECKERBOARD) {
                    color = Colors.shade(color, 0.85F - (fluidCountY * 0.01F)); // darken water color
                }
                color = Colors.mix(color, this.mapWorld.getMapColor(underBlock), 0.20F / (fluidCountY / 2.0F)); // mix block color with water color
                shaded = true;
            }
        } else if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            if (this.mapWorld.config().MAP_LAVA_CHECKERBOARD) {
                color = applyDepthCheckerboard(fluidCountY, color, odd);
                shaded = true;
            }
        }
        return shaded ? color : Colors.removeAlpha(color);
    }

    private static int applyDepthCheckerboard(final double fluidCountY, final int color, final double odd) {
        double diffY = fluidCountY * 0.1D + odd * 0.2D;
        byte colorOffset = (byte) (diffY < 0.5D ? 2 : (diffY > 0.9D ? 0 : 1));
        return Colors.shade(color, colorOffset);
    }

    private @Nullable ChunkSnapshot chunkSnapshot(final ServerLevel level, final int x, final int z) {
        final CompletableFuture<ChunkSnapshot> future = ChunkSnapshot.asyncSnapshot(level, x, z, false);
        while (!future.isDone()) {
            if (this.cancelled) {
                return null;
            }
        }
        return future.join();
    }

    static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignore) {
        }
    }
}
