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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
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
import xyz.jpenilla.squaremap.common.util.ChunkHashMapKey;
import xyz.jpenilla.squaremap.common.util.Colors;
import xyz.jpenilla.squaremap.common.util.Numbers;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.common.util.chunksnapshot.ChunkSnapshotProviderFactory;
import xyz.jpenilla.squaremap.common.util.ConcurrentFIFOLoadingCache;

@DefaultQualifier(NonNull.class)
public abstract class AbstractRender implements Runnable {
    private final ExecutorService executorService;
    private final Executor executor;
    private final Supplier<ChunkSnapshotProvider> createChunkSnapshotProvider;
    private final @Nullable Map<Thread, BiomeColors> biomeColors;
    private ChunkSnapshotManager chunks;
    private volatile @MonotonicNonNull Thread thread;
    protected volatile State state = State.RUNNING;
    protected final MapWorldInternal mapWorld;
    protected final ServerLevel level;

    protected final AtomicInteger processedChunks = new AtomicInteger(0);
    protected final AtomicInteger processedRegions = new AtomicInteger(0);

    protected volatile @Nullable Pair<Timer, RenderProgress> progress = null;

    protected AbstractRender(
        final MapWorldInternal world,
        final ChunkSnapshotProviderFactory chunkSnapshotProviderFactory
    ) {
        this(world, chunkSnapshotProviderFactory, createRenderWorkerPool(world));
    }

    protected AbstractRender(
        final MapWorldInternal mapWorld,
        final ChunkSnapshotProviderFactory chunkSnapshotProviderFactory,
        final ExecutorService workerPool
    ) {
        this.mapWorld = mapWorld;
        this.executorService = workerPool;
        this.executor = new RenderWorkerExecutor(workerPool, this::running);
        this.level = mapWorld.serverLevel();
        this.createChunkSnapshotProvider = () -> chunkSnapshotProviderFactory.createChunkSnapshotProvider(this.level);
        this.chunks = this.createChunkSnapshotManager();
        this.biomeColors = this.mapWorld.config().MAP_BIOMES
            ? new ConcurrentHashMap<>()
            : null; // this should be null if we are not mapping biomes
    }

    private int maximumActiveChunkRequests() {
        final int factor = Integer.getInteger("squaremap.maximumActiveChunkRequestsFactor", 48);
        final int value = ((ThreadPoolExecutor) this.executorService).getCorePoolSize() * factor;
        return Integer.getInteger("squaremap.maximumActiveChunkRequests", value);
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
        this.mapWorld.renderManager().renderStopped(state == State.CANCELLED || state == State.RUNNING);
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

    protected final void clearCaches() {
        this.chunks = this.createChunkSnapshotManager();
        if (this.biomeColors != null) {
            this.biomeColors.clear();
        }
    }

    private ChunkSnapshotManager createChunkSnapshotManager() {
        return new ChunkSnapshotManager(
            this.createChunkSnapshotProvider.get(),
            this.maximumActiveChunkRequests(),
            this.mapWorld.config().MAP_BIOMES_BLEND > 0,
            this::running
        );
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

    protected final CompletableFuture<Void> mapSingleChunk(final Image image, final int chunkX, final int chunkZ) {
        final CompletableFuture<@Nullable ChunkSnapshot> chunkFuture = this.chunks.snapshot(new ChunkPos(chunkX, chunkZ));
        final CompletableFuture<@Nullable ChunkSnapshot> northChunk = this.chunks.snapshotDirect(new ChunkPos(chunkX, chunkZ - 1));

        // queue up the southern chunk in case it was stored with improper yDiff
        // https://github.com/pl3xgaming/Pl3xMap/issues/15
        final CompletableFuture<@Nullable ChunkSnapshot> southChunk;
        final int down = chunkZ + 1;
        if (Numbers.chunkToRegion(chunkZ) == Numbers.chunkToRegion(down)) {
            // Prime left and right (don't need bottom 3 neighbors primed by #snapshot)
            this.chunks.snapshotDirect(new ChunkPos(chunkX + 1, down));
            this.chunks.snapshotDirect(new ChunkPos(chunkX - 1, down));
            southChunk = this.chunks.snapshotDirect(new ChunkPos(chunkX, down));
        } else {
            // chunk belongs to a different region, add to queue
            this.mapWorld.chunkModified(new ChunkCoordinate(chunkX, down));
            southChunk = CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.allOf(northChunk, chunkFuture, southChunk).thenRunAsync(() -> {
            if (!this.running()) {
                return;
            }
            int[] lastY = new int[16];

            // try scanning south row of northern chunk to get proper yDiff
            final @Nullable ChunkSnapshot north = northChunk.join();
            if (north != null) {
                lastY = this.getLastYFromBottomRow(north);
            }

            // scan the chunk itself
            final @Nullable ChunkSnapshot chunk = chunkFuture.join();
            if (chunk != null) {
                this.scanChunk(image, lastY, chunk);
            }

            final @Nullable ChunkSnapshot south = southChunk.join();
            if (south != null) {
                this.scanTopRow(image, lastY, south);
            }

            this.processedChunks.incrementAndGet();
        }, this.executor).exceptionally(thr -> {
            Logging.logger().warn("Exception mapping chunk at [{}, {}] in {}", chunkX, chunkZ, this.mapWorld.identifier().asString(), thr);
            return null;
        });
    }

    protected final CompletableFuture<Void> mapChunkColumn(final Image image, final int chunkX, final int startChunkZ) {
        final List<CompletableFuture<ChunkSnapshot>> futures = new ArrayList<>(33);

        final CompletableFuture<@Nullable ChunkSnapshot> aboveChunkFuture = this.chunks.snapshotDirect(new ChunkPos(chunkX, startChunkZ - 1));
        futures.add(aboveChunkFuture);

        for (int chunkZ = startChunkZ; chunkZ < startChunkZ + 32; chunkZ++) {
            if (!this.mapWorld.visibilityLimit().shouldRenderChunk(chunkX, chunkZ)) {
                // skip rendering this chunk in the chunk column - it's outside the visibility limit
                // (this chunk was already excluded from the chunk count, so not incrementing that is on purpose)
                continue;
            }
            futures.add(this.chunks.snapshot(new ChunkPos(chunkX, chunkZ)));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRunAsync(() -> {
            if (!this.running()) {
                return;
            }
            final int[] lastY = new int[16];
            for (final CompletableFuture<ChunkSnapshot> future : futures) {
                final @Nullable ChunkSnapshot snapshot = future.join();
                if (future == aboveChunkFuture && snapshot != null) {
                    System.arraycopy(this.getLastYFromBottomRow(snapshot), 0, lastY, 0, lastY.length);
                } else if (snapshot != null) {
                    this.scanChunk(image, lastY, snapshot);
                    this.processedChunks.incrementAndGet();
                } else {
                    this.processedChunks.incrementAndGet();
                }
            }
        }, this.executor).exceptionally(thr -> {
            Logging.logger().warn("Exception mapping chunk column starting at [{}, {}] in {}", chunkX, startChunkZ, this.mapWorld.identifier().asString(), thr);
            return null;
        });
    }

    private void scanChunk(final Image image, final int[] lastY, final ChunkSnapshot chunk) {
        while (this.mapWorld.renderManager().rendersPaused() && this.running()) {
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
            color = this.biomeColors.computeIfAbsent(Thread.currentThread(), $ -> new BiomeColors(this.mapWorld, this.chunks))
                .modifyColorFromBiome(color, chunk, mutablePos);
        }

        final int odd = (imgX + imgZ & 1);

        final @Nullable DepthResult fluidDepthResult = findDepthIfFluid(mutablePos, state, chunk);
        if (fluidDepthResult != null) {
            final int fluidDepth = fluidDepthResult.depth;
            final BlockState blockUnder = fluidDepthResult.state;
            return this.getFluidColor(fluidDepth, color, state, blockUnder, odd);
        }

        final int curY = mutablePos.getY();
        final double diffY = ((double) curY - lastY[imgX]) * 4.0D / (double) 4 + ((double) odd - 0.5D) * 0.4D;
        final byte colorOffset = (byte) (diffY > 0.6D ? 2 : (diffY < -0.6D ? 0 : 1));
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
        final Fluid fluid = fluidTypeForRender(color, fluidState.getFluidState());
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

    private static Fluid fluidTypeForRender(final int color, final FluidState fluidState) {
        // treat modded fluids with alpha like water, and those without like lava
        Fluid fluid = fluidState.getType();
        if (fluid != Fluids.WATER && fluid != Fluids.FLOWING_WATER && fluid != Fluids.LAVA && fluid != Fluids.FLOWING_LAVA) {
            final int a = color >> 24 & 255;
            if (a == 255) {
                fluid = Fluids.LAVA;
            } else {
                fluid = Fluids.WATER;
            }
        }
        return fluid;
    }

    private static int applyDepthCheckerboard(final double fluidCountY, final int color, final double odd) {
        double diffY = fluidCountY * 0.1D + odd * 0.2D;
        byte colorOffset = (byte) (diffY < 0.5D ? 2 : (diffY > 0.9D ? 0 : 1));
        return Colors.shade(color, colorOffset);
    }

    private static ExecutorService createRenderWorkerPool(final MapWorldInternal world) {
        return Util.newFixedThreadPool(
            getThreads(world.config().MAX_RENDER_THREADS),
            Util.squaremapThreadFactory("render-worker", world.serverLevel()),
            new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    protected static int getThreads(final int threads) {
        return getThreads(threads, 2);
    }

    protected static int getThreads(int threads, final int factor) {
        if (threads == -1) {
            threads = Runtime.getRuntime().availableProcessors() / factor;
        }
        return Math.max(1, threads);
    }

    protected static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException ignore) {
        }
    }

    public static final class ChunkSnapshotManager {
        private final ChunkSnapshotProvider chunkSnapshotProvider;
        private final int maximumActiveRequests;
        private final ConcurrentFIFOLoadingCache<ChunkHashMapKey, CompletableFuture<@Nullable ChunkSnapshot>> cache;
        public final AtomicLong active = new AtomicLong();
        public final AtomicLong done = new AtomicLong();
        private final boolean biomeBlend;
        private final BooleanSupplier running;

        public ChunkSnapshotManager(
            final ChunkSnapshotProvider chunkSnapshotProvider,
            final int maximumActiveRequests,
            final boolean biomeBlend,
            final BooleanSupplier running
        ) {
            this.chunkSnapshotProvider = chunkSnapshotProvider;
            this.maximumActiveRequests = maximumActiveRequests;
            this.cache = new ConcurrentFIFOLoadingCache<>(
                10 * maximumActiveRequests,
                8 * maximumActiveRequests,
                this::load
            );
            this.biomeBlend = biomeBlend;
            this.running = running;
        }

        private CompletableFuture<ChunkSnapshot> load(final ChunkHashMapKey key) {
            if (!this.maybeWait()) {
                return CompletableFuture.completedFuture(null);
            }
            this.active.incrementAndGet();
            final CompletableFuture<@Nullable ChunkSnapshot> future = this.chunkSnapshotProvider.asyncSnapshot(ChunkPos.getX(key.key), ChunkPos.getZ(key.key));
            future.whenComplete(($, $$) -> this.done.incrementAndGet());
            return future;
        }

        private boolean maybeWait() {
            for (int failures = 1; (this.active.get() - this.done.get()) >= this.maximumActiveRequests; ++failures) {
                if (!this.running.getAsBoolean()) {
                    return false;
                }
                final boolean interrupted = Thread.interrupted();
                Thread.yield();
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(Math.min(10, failures)));
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            return true;
        }

        // requests neighbors when biomes are mapped
        public CompletableFuture<@Nullable ChunkSnapshot> snapshot(final ChunkPos chunkPos) {
            final CompletableFuture<@Nullable ChunkSnapshot> future = this.snapshotDirect(chunkPos);
            if (!this.biomeBlend) {
                return future;
            }

            final int x = chunkPos.x;
            final int z = chunkPos.z;

            this.snapshotDirect(new ChunkPos(x - 1, z - 1));
            this.snapshotDirect(new ChunkPos(x, z - 1));
            this.snapshotDirect(new ChunkPos(x + 1, z + 1));
            this.snapshotDirect(new ChunkPos(x - 1, z));
            this.snapshotDirect(new ChunkPos(x + 1, z));
            this.snapshotDirect(new ChunkPos(x - 1, z + 1));
            this.snapshotDirect(new ChunkPos(x, z + 1));
            this.snapshotDirect(new ChunkPos(x + 1, z - 1));

            // return CompletableFuture.allOf(neighborFutures.toArray(CompletableFuture[]::new)).thenCompose($ -> future);
            return future;
        }

        // only requests the specific chunk
        public CompletableFuture<@Nullable ChunkSnapshot> snapshotDirect(final ChunkPos chunkPos) {
            return this.cache.get(new ChunkHashMapKey(chunkPos));
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
