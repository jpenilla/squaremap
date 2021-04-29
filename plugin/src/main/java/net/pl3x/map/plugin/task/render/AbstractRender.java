package net.pl3x.map.plugin.task.render;

import com.mojang.datafixers.util.Either;
import net.kyori.adventure.text.minimessage.Template;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.BlockStainedGlass;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkProviderServer;
import net.minecraft.server.v1_16_R3.EnumDirection;
import net.minecraft.server.v1_16_R3.FluidType;
import net.minecraft.server.v1_16_R3.FluidTypes;
import net.minecraft.server.v1_16_R3.HeightMap;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.IChunkAccess;
import net.minecraft.server.v1_16_R3.PlayerChunk;
import net.minecraft.server.v1_16_R3.WorldServer;
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
import net.pl3x.map.plugin.visibilitylimit.VisibilityLimit;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractRender implements Runnable {
    private final ExecutorService executor;
    private final FutureTask<Void> futureTask;
    protected volatile boolean cancelled = false;

    protected final MapWorld mapWorld;
    protected final World world;
    protected final WorldServer nmsWorld;
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

                net.minecraft.server.v1_16_R3.Chunk chunk;
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
        }, this.executor);
    }

    protected final @NonNull CompletableFuture<Void> mapSingleChunk(final @NonNull Image image, final int chunkX, final int chunkZ) {
        return CompletableFuture.runAsync(() -> {
            int[] lastY = new int[16];

            net.minecraft.server.v1_16_R3.Chunk chunk;

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
        }, this.executor);
    }

    private void scanChunk(Image image, int[] lastY, Chunk chunk) {
        while (mapWorld.rendersPaused()) {
            sleep(500);
        }
        final int blockX = chunk.getPos().getBlockX();
        final int blockZ = chunk.getPos().getBlockZ();
        VisibilityLimit limit = mapWorld.visibilityLimit();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (cancelled) return;

                if (limit.shouldRenderColumn(blockX + x, blockZ + z)) {
                    image.setPixel(blockX + x, blockZ + z, scanBlock(chunk, x, z, lastY));
                }
            }
        }
    }

    private void scanTopRow(Image image, int[] lastY, Chunk chunk) {
        final int blockX = chunk.getPos().getBlockX();
        final int blockZ = chunk.getPos().getBlockZ();
        for (int x = 0; x < 16; x++) {
            if (cancelled) return;
            image.setPixel(blockX + x, blockZ, scanBlock(chunk, x, 0, lastY));
        }
    }

    private int @NonNull [] getLastYFromBottomRow(final @NonNull Chunk chunk) {
        final int[] lastY = new int[16];
        final BlockPosition.MutableBlockPosition mutablePos = new BlockPosition.MutableBlockPosition();
        for (int x = 0; x < 16; x++) {
            if (cancelled) return lastY;
            final int yDiff = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, x, 15) + 1;
            int height = mapWorld.config().MAP_MAX_HEIGHT == -1 ? chunk.getWorld().getBuildHeight() : mapWorld.config().MAP_MAX_HEIGHT;
            mutablePos.setValues(chunk.getPos().getBlockX() + x, Math.min(yDiff, height), chunk.getPos().getBlockZ() + 15);
            final IBlockData state = mapWorld.config().MAP_ITERATE_UP ? iterateUp(chunk, mutablePos) : iterateDown(chunk, mutablePos);
            if (mapWorld.config().MAP_GLASS_CLEAR && isGlass(state)) {
                handleGlass(chunk, mutablePos);
            }
            lastY[x] = mutablePos.getY();
        }
        return lastY;
    }

    private int scanBlock(Chunk chunk, int imgX, int imgZ, int[] lastY) {
        int blockX = chunk.getPos().getBlockX() + imgX;
        int blockZ = chunk.getPos().getBlockZ() + imgZ;

        IBlockData state;
        final BlockPosition.MutableBlockPosition mutablePos = new BlockPosition.MutableBlockPosition();

        final int yDiff = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, imgX, imgZ) + 1;
        int height = mapWorld.config().MAP_MAX_HEIGHT == -1 ? chunk.getWorld().getBuildHeight() : mapWorld.config().MAP_MAX_HEIGHT;
        mutablePos.setValues(blockX, Math.min(yDiff, height), blockZ);

        if (yDiff > 1) {
            state = mapWorld.config().MAP_ITERATE_UP ? iterateUp(chunk, mutablePos) : iterateDown(chunk, mutablePos);
        } else {
            // no blocks found, show invisible/air
            return Colors.clearMapColor().rgb;
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

    private int getColor(final @NonNull Chunk chunk, final int imgX, final int imgZ, final int[] lastY, final @NonNull IBlockData state, final BlockPosition.@NonNull MutableBlockPosition mutablePos) {
        int color = mapWorld.getMapColor(state);

        if (this.biomeColors != null) {
            color = this.biomeColors.get().modifyColorFromBiome(color, chunk, mutablePos);
        }

        int odd = (imgX + imgZ & 1);

        final Pair<Integer, IBlockData> fluidPair = findDepthIfFluid(mutablePos, state, chunk);
        if (fluidPair != null) {
            final int fluidDepth = fluidPair.left();
            final IBlockData blockUnder = fluidPair.right();
            return this.getFluidColor(fluidDepth, color, state, blockUnder, odd);
        }

        final int curY = mutablePos.getY();
        double diffY = ((double) curY - lastY[imgX]) * 4.0D / (double) 4 + ((double) odd - 0.5D) * 0.4D;
        byte colorOffset = (byte) (diffY > 0.6D ? 2 : (diffY < -0.6D ? 0 : 1));
        lastY[imgX] = curY;
        return Colors.shade(color, colorOffset);
    }

    private @NonNull IBlockData iterateDown(final @NonNull Chunk chunk, final BlockPosition.@NonNull MutableBlockPosition mutablePos) {
        IBlockData state;
        if (chunk.getWorld().getDimensionManager().hasCeiling()) {
            do {
                mutablePos.c(EnumDirection.DOWN);
                state = chunk.getType(mutablePos);
            } while (!state.isAir() && mutablePos.getY() > 0);
        }
        do {
            mutablePos.c(EnumDirection.DOWN);
            state = chunk.getType(mutablePos);
        } while ((mapWorld.getMapColor(state) == Colors.clearMapColor().rgb || mapWorld.advanced().invisibleBlocks.contains(state.getBlock())) && mutablePos.getY() > 0);
        return state;
    }

    private @NonNull IBlockData iterateUp(final @NonNull Chunk chunk, final BlockPosition.@NonNull MutableBlockPosition mutablePos) {
        IBlockData state;
        int height = mutablePos.getY();
        mutablePos.setY(0);
        if (chunk.getWorld().getDimensionManager().hasCeiling()) {
            do {
                mutablePos.c(EnumDirection.UP);
                state = chunk.getType(mutablePos);
            } while (!state.isAir() && mutablePos.getY() < height);
            do {
                mutablePos.c(EnumDirection.UP);
                state = chunk.getType(mutablePos);
            } while (!mapWorld.advanced().iterateUpBaseBlocks.contains(state.getBlock()) && mutablePos.getY() < height);
        }
        do {
            mutablePos.c(EnumDirection.DOWN);
            state = chunk.getType(mutablePos);
        } while ((mapWorld.getMapColor(state) == Colors.clearMapColor().rgb || mapWorld.advanced().invisibleBlocks.contains(state.getBlock())) && mutablePos.getY() > 0);
        return state;
    }

    private static boolean isGlass(final @NonNull IBlockData state) {
        final Block block = state.getBlock();
        return block == Blocks.GLASS || block instanceof BlockStainedGlass;
    }

    private @NonNull IBlockData handleGlass(final @NonNull Chunk chunk, final BlockPosition.@NonNull MutableBlockPosition mutablePos) {
        IBlockData state = chunk.getType(mutablePos);
        while (isGlass(state)) {
            state = iterateDown(chunk, mutablePos);
        }
        return state;
    }

    private static @Nullable Pair<Integer, IBlockData> findDepthIfFluid(final @NonNull BlockPosition blockPos, final @NonNull IBlockData state, final @NonNull Chunk chunk) {
        if (blockPos.getY() > 0 && !state.getFluid().isEmpty()) {
            IBlockData fluidState;
            int fluidDepth = 0;

            int yBelowSurface = blockPos.getY() - 1;
            final BlockPosition.MutableBlockPosition mutablePos = new BlockPosition.MutableBlockPosition();
            mutablePos.setValues(blockPos);
            do {
                mutablePos.setY(yBelowSurface--);
                fluidState = chunk.getType(mutablePos);
                ++fluidDepth;
            } while (yBelowSurface > 0 && fluidDepth <= 10 && !fluidState.getFluid().isEmpty());

            return Pair.of(fluidDepth, fluidState);
        }
        return null;
    }

    private int getFluidColor(final int fluidCountY, int color, final @NonNull IBlockData fluidState, final @NonNull IBlockData underBlock, final int odd) {
        final FluidType fluid = fluidState.getFluid().getType();
        boolean shaded = false;
        if (fluid == FluidTypes.WATER || fluid == FluidTypes.FLOWING_WATER) {
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
        } else if (fluid == FluidTypes.LAVA || fluid == FluidTypes.FLOWING_LAVA) {
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

    private net.minecraft.server.v1_16_R3.Chunk getChunkAt(net.minecraft.server.v1_16_R3.World world, int x, int z) {
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        net.minecraft.server.v1_16_R3.Chunk ifLoaded = provider.getChunkAtIfLoadedImmediately(x, z);
        if (ifLoaded != null) {
            return ifLoaded;
        }
        CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> future = provider.getChunkAtAsynchronously(x, z, false, true);
        while (!future.isDone()) {
            if (cancelled) return null;
        }
        return (Chunk) future.join().left().orElse(null);
    }

    void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignore) {
        }
    }
}
