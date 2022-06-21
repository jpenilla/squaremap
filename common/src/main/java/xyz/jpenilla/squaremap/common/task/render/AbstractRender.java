package xyz.jpenilla.squaremap.common.task.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
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
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.Pair;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.data.BiomeColors;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.data.Image;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.data.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.common.util.Colors;
import xyz.jpenilla.squaremap.common.util.Numbers;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public abstract class AbstractRender implements Runnable {
    private final ExecutorService executorService;
    private final Executor executor;
    private final ChunkSnapshotProvider chunkSnapshotProvider;
    private volatile @MonotonicNonNull Thread thread;
    protected volatile State state = State.RUNNING;

    protected final MapWorldInternal mapWorld;
    protected final ServerLevel level;

    protected final @Nullable Map<Thread, BiomeColors> biomeColors;

    protected final AtomicInteger processedChunks = new AtomicInteger(0);
    protected final AtomicInteger processedRegions = new AtomicInteger(0);

    protected volatile @Nullable Pair<Timer, RenderProgress> progress = null;

    protected AbstractRender(
        final MapWorldInternal world,
        final ChunkSnapshotProvider chunkSnapshotProvider
    ) {
        this(world, chunkSnapshotProvider, createRenderWorkerPool(world));
    }

    protected AbstractRender(
        final MapWorldInternal mapWorld,
        final ChunkSnapshotProvider chunkSnapshotProvider,
        final ExecutorService workerPool
    ) {
        this.mapWorld = mapWorld;
        this.executorService = workerPool;
        this.executor = new RenderWorkerExecutor(workerPool, this::running);
        this.level = mapWorld.serverLevel();
        this.chunkSnapshotProvider = chunkSnapshotProvider;
        this.biomeColors = this.mapWorld.config().MAP_BIOMES
            ? new ConcurrentHashMap<>()
            : null; // this should be null if we are not mapping biomes
    }

    protected abstract void render();

    protected final boolean running() {
        return this.state == State.RUNNING;
    }

    @Override
    public final void run() {
        if (!this.running()) {
            return;
        }

        this.thread = Thread.currentThread();

        try {
            this.render();
        } catch (final Exception ex) {
            Logging.logger().warn("Encountered exception executing render", ex);
        }

        this.renderStopped();
    }

    private void renderStopped() {
        if (this instanceof BackgroundRender) {
            return;
        }
        final State state = this.state;
        this.shutdown();
        this.mapWorld.renderStopped(state == State.CANCELLED || state == State.RUNNING);
        final String msg = state == State.RUNNING ? Messages.LOG_FINISHED_RENDERING : Messages.LOG_CANCELLED_RENDERING;
        Logging.info(msg, "world", this.mapWorld.identifier().asString());
    }

    private synchronized void shutdown() {
        if (this.progress != null) {
            this.progress.left().cancel();
            this.progress = null;
        }

        if (!this.executorService.isShutdown()) {
            this.executorService.shutdownNow();
        }
    }

    public final void stop() {
        this.stop(State.STOPPED);
    }

    public final void cancel() {
        this.stop(State.CANCELLED);
    }

    private void stop(final State state) {
        if (this.state != State.RUNNING) {
            throw new IllegalStateException("Stop already requested");
        }
        this.state = state;
        this.shutdown();
        final Thread thread = this.thread;
        if (thread != null) {
            thread.interrupt();
        } else {
            this.renderStopped();
        }
    }

    public abstract int totalChunks();

    public abstract int totalRegions();

    public final int processedChunks() {
        return this.processedChunks.get();
    }

    public final int processedRegions() {
        return this.processedRegions.get();
    }

    public final void restartProgressLogger() {
        final @Nullable RenderProgress old;
        final @Nullable Pair<Timer, RenderProgress> progress = this.progress;
        if (progress != null) {
            progress.left().cancel();
            old = progress.right();
            this.progress = RenderProgress.printProgress(this, old);
        }
    }

    protected final void mapRegion(final RegionCoordinate region) {
        final Image image = new Image(region, this.mapWorld.tilesPath(), this.mapWorld.config().ZOOM_MAX);
        final int startX = region.getChunkX();
        final int startZ = region.getChunkZ();
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int chunkX = startX; chunkX < startX + 32; chunkX++) {
            futures.add(this.mapChunkColumn(image, chunkX, startZ));
        }
        for (final CompletableFuture<Void> future : futures) {
            try {
                future.get();
            } catch (final InterruptedException ignore) {
                return;
            } catch (final CancellationException | ExecutionException ex) {
                Logging.logger().warn("Exception mapping region {}", region, ex);
            }
        }
        if (this.running()) {
            this.mapWorld.saveImage(image);
        }
    }

    protected final CompletableFuture<Void> mapChunkColumn(final Image image, final int chunkX, final int startChunkZ) {
        return CompletableFuture.runAsync(() -> {
            int[] lastY = new int[16];
            for (int chunkZ = startChunkZ; chunkZ < startChunkZ + 32; chunkZ++) {
                if (!this.running()) {
                    return;
                }
                if (!this.mapWorld.visibilityLimit().shouldRenderChunk(chunkX, chunkZ)) {
                    // skip rendering this chunk in the chunk column - it's outside the visibility limit
                    // (this chunk was already excluded from the chunk count, so not incrementing that is on purpose)
                    continue;
                }

                @Nullable ChunkSnapshot chunk;
                if (chunkZ == startChunkZ) {
                    // this is the top line of the image, we need to
                    // scan the bottom line of the region to the north
                    // in order to get the correct lastY for shading
                    chunk = this.chunkSnapshot(chunkX, chunkZ - 1);
                    if (chunk != null) {
                        lastY = this.getLastYFromBottomRow(chunk);
                    }
                }
                chunk = this.chunkSnapshot(chunkX, chunkZ);
                if (chunk != null) {
                    this.scanChunk(image, lastY, chunk);
                }
                this.processedChunks.incrementAndGet();
            }
        }, this.executor).exceptionally(thr -> {
            Logging.logger().warn("Exception mapping chunk column starting at [{}, {}] in {}", chunkX, startChunkZ, this.mapWorld.identifier().asString(), thr);
            return null;
        });
    }

    protected final CompletableFuture<Void> mapSingleChunk(final Image image, final int chunkX, final int chunkZ) {
        return CompletableFuture.runAsync(() -> {
            int[] lastY = new int[16];

            @Nullable ChunkSnapshot chunk;

            // try scanning south row of northern chunk to get proper yDiff
            chunk = this.chunkSnapshot(chunkX, chunkZ - 1);
            if (chunk != null) {
                lastY = this.getLastYFromBottomRow(chunk);
            }

            // scan the chunk itself
            chunk = this.chunkSnapshot(chunkX, chunkZ);
            if (chunk != null) {
                this.scanChunk(image, lastY, chunk);
            }

            // queue up the southern chunk in case it was stored with improper yDiff
            // https://github.com/pl3xgaming/Pl3xMap/issues/15
            final int down = chunkZ + 1;
            if (Numbers.chunkToRegion(chunkZ) == Numbers.chunkToRegion(down)) {
                chunk = this.chunkSnapshot(chunkX, down);
                if (chunk != null) {
                    this.scanTopRow(image, lastY, chunk);
                }
            } else {
                // chunk belongs to a different region, add to queue
                this.mapWorld.chunkModified(new ChunkCoordinate(chunkX, down));
            }

            this.processedChunks.incrementAndGet();
        }, this.executor).exceptionally(thr -> {
            Logging.logger().warn("Exception mapping chunk at [{}, {}] in {}", chunkX, chunkZ, this.mapWorld.identifier().asString(), thr);
            return null;
        });
    }

    private void scanChunk(final Image image, final int[] lastY, final ChunkSnapshot chunk) {
        while (this.mapWorld.rendersPaused() && this.running()) {
            sleep(500);
        }
        final int blockX = chunk.pos().getMinBlockX();
        final int blockZ = chunk.pos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (!this.running()) {
                    return;
                }
                if (this.mapWorld.visibilityLimit().shouldRenderColumn(blockX + x, blockZ + z)) {
                    image.setPixel(blockX + x, blockZ + z, this.scanBlock(chunk, x, z, lastY));
                }
            }
        }
    }

    private void scanTopRow(final Image image, final int[] lastY, final ChunkSnapshot chunk) {
        final int blockX = chunk.pos().getMinBlockX();
        final int blockZ = chunk.pos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            if (!this.running()) {
                return;
            }
            if (this.mapWorld.visibilityLimit().shouldRenderColumn(blockX + x, blockZ)) {
                image.setPixel(blockX + x, blockZ, this.scanBlock(chunk, x, 0, lastY));
            }
        }
    }

    private int effectiveMaxHeight(final ChunkSnapshot chunk) {
        return this.mapWorld.config().MAP_MAX_HEIGHT == -1
            ? chunk.getMaxBuildHeight()
            : this.mapWorld.config().MAP_MAX_HEIGHT;
    }

    private int[] getLastYFromBottomRow(final ChunkSnapshot chunk) {
        final int[] lastY = new int[16];
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            if (!this.running()) {
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

    private int getColor(final ChunkSnapshot chunk, final int imgX, final int imgZ, final int[] lastY, final BlockState state, final BlockPos.MutableBlockPos mutablePos) {
        int color = this.mapWorld.getMapColor(state);

        if (this.biomeColors != null) {
            color = this.biomeColors.computeIfAbsent(Thread.currentThread(), $ -> new BiomeColors(this.mapWorld, this.chunkSnapshotProvider))
                .modifyColorFromBiome(color, chunk, mutablePos);
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

    private BlockState iterateDown(final ChunkSnapshot chunk, final BlockPos.MutableBlockPos mutablePos) {
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

    private BlockState iterateUp(final ChunkSnapshot chunk, final BlockPos.MutableBlockPos mutablePos) {
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

    private static boolean isGlass(final BlockState state) {
        final Block block = state.getBlock();
        return block == Blocks.GLASS || block instanceof StainedGlassBlock;
    }

    private BlockState handleGlass(final ChunkSnapshot chunk, final BlockPos.MutableBlockPos mutablePos) {
        BlockState state = chunk.getBlockState(mutablePos);
        while (isGlass(state)) {
            state = this.iterateDown(chunk, mutablePos);
        }
        return state;
    }

    private record DepthResult(int depth, BlockState state) {
    }

    private static @Nullable DepthResult findDepthIfFluid(final BlockPos blockPos, final BlockState state, final ChunkSnapshot chunk) {
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

    private int getFluidColor(final int fluidCountY, int color, final BlockState fluidState, final BlockState underBlock, final int odd) {
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

    private @Nullable ChunkSnapshot chunkSnapshot(final int x, final int z) {
        final CompletableFuture<ChunkSnapshot> future = this.chunkSnapshotProvider.asyncSnapshot(this.level, x, z, false);
        for (int failures = 1; !future.isDone(); ++failures) {
            if (!this.running()) {
                return null;
            }
            boolean interrupted = Thread.interrupted();
            Thread.yield();
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(Math.min(5, failures)));
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        return future.join();
    }

    private static ExecutorService createRenderWorkerPool(final MapWorldInternal world) {
        return Util.newFixedThreadPool(
            getThreads(world.config().MAX_RENDER_THREADS),
            Util.squaremapThreadFactory("render-worker", world.serverLevel()),
            new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    protected static int getThreads(int threads) {
        if (threads == -1) {
            threads = Runtime.getRuntime().availableProcessors() / 3;
        }
        return Math.max(1, threads);
    }

    protected static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException ignore) {
        }
    }

    private record RenderWorkerExecutor(Executor wrapped, BooleanSupplier running) implements Executor {
        @Override
        public void execute(final Runnable task) {
            this.wrapped.execute(new WorkerTask(task, this.running));
        }

        private record WorkerTask(Runnable wrapped, BooleanSupplier running) implements Runnable {
            @Override
            public void run() {
                if (this.running.getAsBoolean()) {
                    this.wrapped.run();
                }
            }
        }
    }

    protected enum State {
        RUNNING,
        STOPPED,
        CANCELLED
    }
}
