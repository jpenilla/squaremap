package net.pl3x.map.plugin.task.render;

import com.mojang.datafixers.util.Either;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.minimessage.Template;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.pl3x.map.api.Pair;
import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.data.BiomeColors;
import net.pl3x.map.plugin.data.ChunkCoordinate;
import net.pl3x.map.plugin.data.Image;
import net.pl3x.map.plugin.data.MapWorld;
import net.pl3x.map.plugin.data.Region;
import net.pl3x.map.plugin.util.Colors;
import net.pl3x.map.plugin.util.FileUtil;
import net.pl3x.map.plugin.util.Numbers;
import org.apache.logging.log4j.LogManager;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractRender implements Runnable {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();

    private final ExecutorService executor;
    private final FutureTask<Void> futureTask;
    protected volatile boolean cancelled = false;

    protected final MapWorld mapWorld;
    protected final World world;
    protected final ServerLevel nmsWorld;
    protected final Path worldTilesDir;

    private final ThreadLocal<BiomeColors> biomeColors;

    protected final AtomicInteger curChunks = new AtomicInteger(0);
    protected final AtomicInteger curRegions = new AtomicInteger(0);

    protected Timer timer = null;

    public AbstractRender(final @NonNull MapWorld mapWorld) {
        this(mapWorld, Executors.newFixedThreadPool(getThreads(mapWorld.config().MAX_RENDER_THREADS)));
    }

    public AbstractRender(final @NonNull MapWorld mapWorld, final @NonNull ExecutorService executor) {
        this.futureTask = new FutureTask<>(this, null);
        this.mapWorld = mapWorld;
        this.executor = executor;
        this.world = mapWorld.bukkit();
        this.nmsWorld = ((CraftWorld) this.world).getHandle();
        this.worldTilesDir = FileUtil.getWorldFolder(world);
        this.biomeColors = this.mapWorld.config().MAP_BIOMES
                ? ThreadLocal.withInitial(() -> new BiomeColors(mapWorld))
                : null; // this should be null if we are not mapping biomes
    }

    public static int getThreads(int threads) {
        if (threads == -1) {
            threads = Runtime.getRuntime().availableProcessors() / 2;
        }
        return Math.max(1, threads);
    }

    public synchronized void cancel() {
        if (this.timer != null) {
            this.timer.cancel();
        }
        this.executor.shutdown();
        this.cancelled = true;
        this.futureTask.cancel(false);
    }

    @Override
    public final void run() {
        this.render();

        if (!(this instanceof BackgroundRender)) {
            final boolean finished = !cancelled;

            this.mapWorld.stopRender();

            if (finished) {
                this.mapWorld.finishedRender();
                Logger.info(Lang.LOG_FINISHED_RENDERING, Template.of("world", world.getName()));
            } else {
                Logger.info(Lang.LOG_CANCELLED_RENDERING, Template.of("world", world.getName()));
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

    protected final void mapRegion(final @NonNull Region region) {
        Image image = new Image(region, worldTilesDir, mapWorld.config().ZOOM_MAX);
        int startX = region.getChunkX();
        int startZ = region.getChunkZ();
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int chunkX = startX; chunkX < startX + 32; chunkX++) {
            futures.add(this.mapChunkColumn(image, chunkX, startZ));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        LOGGER.warn("Exception mapping region!", throwable);
                        return;
                    }
                    if (!this.cancelled) {
                        this.mapWorld.saveImage(image);
                    }
                })
                .join();
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

                net.minecraft.world.level.chunk.LevelChunk chunk;
                if (chunkZ == startChunkZ) {
                    // this is the top line of the image, we need to
                    // scan the bottom line of the region to the north
                    // in order to get the correct lastY for shading
                    chunk = getChunkAt(nmsWorld, chunkX, chunkZ - 1);
                    if (chunk != null && !chunk.isEmpty()) {
                        lastY = getLastYFromBottomRow(chunk);
                    }
                }
                chunk = getChunkAt(nmsWorld, chunkX, chunkZ);
                if (chunk != null && !chunk.isEmpty()) {
                    scanChunk(image, lastY, chunk);
                }
                curChunks.incrementAndGet();
            }
        }, this.executor).exceptionally(thr -> {
            LOGGER.warn("mapChunkColumn failed!", thr);
            return null;
        });
    }

    protected final @NonNull CompletableFuture<Void> mapSingleChunk(final @NonNull Image image, final int chunkX, final int chunkZ) {
        return CompletableFuture.runAsync(() -> {
            int[] lastY = new int[16];

            net.minecraft.world.level.chunk.LevelChunk chunk;

            // try scanning south row of northern chunk to get proper yDiff
            chunk = getChunkAt(nmsWorld, chunkX, chunkZ - 1);
            if (chunk != null && !chunk.isEmpty()) {
                lastY = getLastYFromBottomRow(chunk);
            }

            // scan the chunk itself
            chunk = getChunkAt(nmsWorld, chunkX, chunkZ);
            if (chunk != null && !chunk.isEmpty()) {
                scanChunk(image, lastY, chunk);
            }

            // queue up the southern chunk in case it was stored with improper yDiff
            // https://github.com/pl3xgaming/Pl3xMap/issues/15
            final int down = chunkZ + 1;
            chunk = getChunkAt(nmsWorld, chunkX, down);
            if (chunk != null && !chunk.isEmpty()) {
                if (Numbers.chunkToRegion(chunkZ) == Numbers.chunkToRegion(down)) {
                    scanTopRow(image, lastY, chunk);
                } else {
                    // chunk belongs to a different region, add to queue
                    mapWorld.chunkModified(new ChunkCoordinate(chunkX, down));
                }
            }

            curChunks.incrementAndGet();
        }, this.executor).exceptionally(thr -> {
            LOGGER.warn("mapSingleChunk failed!", thr);
            return null;
        });
    }

    private void scanChunk(Image image, int[] lastY, LevelChunk chunk) {
        while (mapWorld.rendersPaused()) {
            sleep(500);
        }
        final int blockX = chunk.getPos().getMinBlockX();
        final int blockZ = chunk.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (cancelled) return;
                if (mapWorld.visibilityLimit().shouldRenderColumn(blockX + x, blockZ + z)) {
                    image.setPixel(blockX + x, blockZ + z, scanBlock(chunk, x, z, lastY));
                }
            }
        }
    }

    private void scanTopRow(Image image, int[] lastY, LevelChunk chunk) {
        final int blockX = chunk.getPos().getMinBlockX();
        final int blockZ = chunk.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            if (cancelled) return;
            if (mapWorld.visibilityLimit().shouldRenderColumn(blockX + x, blockZ)) {
                image.setPixel(blockX + x, blockZ, scanBlock(chunk, x, 0, lastY));
            }
        }
    }

    private int @NonNull [] getLastYFromBottomRow(final @NonNull LevelChunk chunk) {
        final int[] lastY = new int[16];
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            if (cancelled) return lastY;
            final int yDiff = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, 15) + 1;
            int height = mapWorld.config().MAP_MAX_HEIGHT == -1 ? chunk.getLevel().getLogicalHeight() : mapWorld.config().MAP_MAX_HEIGHT;
            mutablePos.set(
                    chunk.getPos().getMinBlockX() + x,
                    Math.min(yDiff, height),
                    chunk.getPos().getMinBlockZ() + 15
            );
            final BlockState state = mapWorld.config().MAP_ITERATE_UP ? iterateUp(chunk, mutablePos) : iterateDown(chunk, mutablePos);
            if (mapWorld.config().MAP_GLASS_CLEAR && isGlass(state)) {
                handleGlass(chunk, mutablePos);
            }
            lastY[x] = mutablePos.getY();
        }
        return lastY;
    }

    private int scanBlock(LevelChunk chunk, int imgX, int imgZ, int[] lastY) {
        int blockX = chunk.getPos().getMinBlockX() + imgX;
        int blockZ = chunk.getPos().getMinBlockZ() + imgZ;

        BlockState state;
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        final int yDiff = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, imgX, imgZ) + 1;
        int height = mapWorld.config().MAP_MAX_HEIGHT == -1 ? chunk.getLevel().getLogicalHeight() : mapWorld.config().MAP_MAX_HEIGHT;
        mutablePos.set(blockX, Math.min(yDiff, height), blockZ);

        if (yDiff > 1) {
            state = mapWorld.config().MAP_ITERATE_UP ? iterateUp(chunk, mutablePos) : iterateDown(chunk, mutablePos);
        } else {
            // no blocks found, show invisible/air
            return Colors.clearMapColor();
        }

        if (mapWorld.config().MAP_GLASS_CLEAR && isGlass(state)) {
            final int glassColor = mapWorld.getMapColor(state);
            final float glassAlpha = state.getBlock() == Blocks.GLASS ? 0.25F : 0.5F;
            state = handleGlass(chunk, mutablePos);
            final int color = getColor(chunk, imgX, imgZ, lastY, state, mutablePos);
            return Colors.mix(color, glassColor, glassAlpha);
        }

        return getColor(chunk, imgX, imgZ, lastY, state, mutablePos);
    }

    private int getColor(final @NonNull LevelChunk chunk, final int imgX, final int imgZ, final int[] lastY, final @NonNull BlockState state, final BlockPos.@NonNull MutableBlockPos mutablePos) {
        int color = mapWorld.getMapColor(state);

        if (this.biomeColors != null) {
            color = this.biomeColors.get().modifyColorFromBiome(color, chunk, mutablePos);
        }

        int odd = (imgX + imgZ & 1);

        final Pair<Integer, BlockState> fluidPair = findDepthIfFluid(mutablePos, state, chunk);
        if (fluidPair != null) {
            final int fluidDepth = fluidPair.left();
            final BlockState blockUnder = fluidPair.right();
            return this.getFluidColor(fluidDepth, color, state, blockUnder, odd);
        }

        final int curY = mutablePos.getY();
        double diffY = ((double) curY - lastY[imgX]) * 4.0D / (double) 4 + ((double) odd - 0.5D) * 0.4D;
        byte colorOffset = (byte) (diffY > 0.6D ? 2 : (diffY < -0.6D ? 0 : 1));
        lastY[imgX] = curY;
        return Colors.shade(color, colorOffset);
    }

    private @NonNull BlockState iterateDown(final @NonNull LevelChunk chunk, final BlockPos.@NonNull MutableBlockPos mutablePos) {
        BlockState state;
        if (chunk.getLevel().dimensionType().hasCeiling()) {
            do {
                mutablePos.move(Direction.DOWN);
                state = chunk.getBlockState(mutablePos);
            } while (!state.isAir() && mutablePos.getY() > 0);
        }
        do {
            mutablePos.move(Direction.DOWN);
            state = chunk.getBlockState(mutablePos);
        } while ((mapWorld.getMapColor(state) == Colors.clearMapColor() || mapWorld.advanced().invisibleBlocks.contains(state.getBlock())) && mutablePos.getY() > 0);
        return state;
    }

    private @NonNull BlockState iterateUp(final @NonNull LevelChunk chunk, final BlockPos.@NonNull MutableBlockPos mutablePos) {
        BlockState state;
        int height = mutablePos.getY();
        mutablePos.setY(0);
        if (chunk.getLevel().dimensionType().hasCeiling()) {
            do {
                mutablePos.move(Direction.UP);
                state = chunk.getBlockState(mutablePos);
            } while (!state.isAir() && mutablePos.getY() < height);
            do {
                mutablePos.move(Direction.UP);
                state = chunk.getBlockState(mutablePos);
            } while (!mapWorld.advanced().iterateUpBaseBlocks.contains(state.getBlock()) && mutablePos.getY() < height);
        }
        do {
            mutablePos.move(Direction.DOWN);
            state = chunk.getBlockState(mutablePos);
        } while ((mapWorld.getMapColor(state) == Colors.clearMapColor() || mapWorld.advanced().invisibleBlocks.contains(state.getBlock())) && mutablePos.getY() > 0);
        return state;
    }

    private static boolean isGlass(final @NonNull BlockState state) {
        final Block block = state.getBlock();
        return block == Blocks.GLASS || block instanceof StainedGlassBlock;
    }

    private @NonNull BlockState handleGlass(final @NonNull LevelChunk chunk, final BlockPos.@NonNull MutableBlockPos mutablePos) {
        BlockState state = chunk.getBlockState(mutablePos);
        while (isGlass(state)) {
            state = iterateDown(chunk, mutablePos);
        }
        return state;
    }

    private static @Nullable Pair<Integer, BlockState> findDepthIfFluid(final @NonNull BlockPos blockPos, final @NonNull BlockState state, final @NonNull LevelChunk chunk) {
        if (blockPos.getY() > 0 && !state.getFluidState().isEmpty()) {
            BlockState fluidState;
            int fluidDepth = 0;

            int yBelowSurface = blockPos.getY() - 1;
            final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            mutablePos.set(blockPos);
            do {
                mutablePos.setY(yBelowSurface--);
                fluidState = chunk.getBlockState(mutablePos);
                ++fluidDepth;
            } while (yBelowSurface > 0 && fluidDepth <= 10 && !fluidState.getFluidState().isEmpty());

            return Pair.of(fluidDepth, fluidState);
        }
        return null;
    }

    private int getFluidColor(final int fluidCountY, int color, final @NonNull BlockState fluidState, final @NonNull BlockState underBlock, final int odd) {
        final Fluid fluid = fluidState.getFluidState().getType();
        boolean shaded = false;
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            if (mapWorld.config().MAP_WATER_CHECKERBOARD) {
                color = applyDepthCheckerboard(fluidCountY, color, odd);
                shaded = true;
            }
            if (mapWorld.config().MAP_WATER_CLEAR) {
                if (!mapWorld.config().MAP_WATER_CHECKERBOARD) {
                    color = Colors.shade(color, 0.85F - (fluidCountY * 0.01F)); // darken water color
                }
                color = Colors.mix(color, mapWorld.getMapColor(underBlock), 0.20F / (fluidCountY / 2.0F)); // mix block color with water color
                shaded = true;
            }
        } else if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            if (mapWorld.config().MAP_LAVA_CHECKERBOARD) {
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

    private net.minecraft.world.level.chunk.LevelChunk getChunkAt(ServerLevel world, int x, int z) {
        final ServerChunkCache chunkCache = world.getChunkSource();
        net.minecraft.world.level.chunk.LevelChunk ifLoaded = chunkCache.getChunkAtIfLoadedImmediately(x, z);
        if (ifLoaded != null) {
            return ifLoaded;
        }
        final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = chunkCache.getChunkAtAsynchronously(x, z, false, true);
        while (!future.isDone()) {
            if (cancelled) return null;
        }
        return (LevelChunk) future.join().left().orElse(null);
    }

    void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignore) {
        }
    }
}
