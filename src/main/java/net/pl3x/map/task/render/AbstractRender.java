package net.pl3x.map.task.render;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
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
import net.pl3x.map.configuration.WorldConfig;
import net.pl3x.map.data.Image;
import net.pl3x.map.data.MapWorld;
import net.pl3x.map.data.Region;
import net.pl3x.map.util.BiomeColors;
import net.pl3x.map.util.Colors;
import net.pl3x.map.util.FileUtil;
import net.pl3x.map.util.Pair;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractRender implements Runnable {
    private static final Set<Block> invisibleBlocks = ImmutableSet.of(
            Blocks.TALL_GRASS,
            Blocks.FERN,
            Blocks.GRASS,
            Blocks.LARGE_FERN
    );

    private final ExecutorService executor;
    private final FutureTask<Void> futureTask;
    protected volatile boolean cancelled = false;

    protected final MapWorld mapWorld;
    protected final World world;
    protected final WorldServer nmsWorld;
    protected final WorldConfig worldConfig;
    protected final Path worldTilesDir;

    private final ThreadLocal<BiomeColors> biomeColors;

    protected final AtomicInteger curChunks = new AtomicInteger(0);

    public AbstractRender(final @NonNull MapWorld mapWorld) {
        this.futureTask = new FutureTask<>(this, null);
        this.mapWorld = mapWorld;
        this.worldConfig = mapWorld.config();
        this.executor = Executors.newFixedThreadPool(this.worldConfig.MAX_RENDER_THREADS);
        this.world = mapWorld.bukkit();
        this.nmsWorld = ((CraftWorld) this.world).getHandle();
        this.worldTilesDir = FileUtil.getWorldFolder(world);
        this.biomeColors = this.worldConfig.MAP_BIOMES
                ? ThreadLocal.withInitial(() -> BiomeColors.forWorld(nmsWorld))
                : null; // this should be null if we are not mapping biomes
    }

    public synchronized void cancel() {
        this.executor.shutdown();
        this.cancelled = true;
        this.futureTask.cancel(false);
    }

    @Override
    public final void run() {
        this.render();

        if (!(this instanceof BackgroundRender)) {
            this.mapWorld.stopRender();
        }
    }

    public abstract int totalChunks();

    public final int processedChunks() {
        return this.curChunks.get();
    }

    protected abstract void render();

    public final @NonNull FutureTask<Void> getFutureTask() {
        return this.futureTask;
    }

    protected final void mapRegion(final @NonNull Region region) {
        Image image = new Image(region, worldTilesDir, worldConfig.ZOOM_MAX);
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
                net.minecraft.server.v1_16_R3.Chunk chunk;
                if (chunkZ == startChunkZ) {
                    // this is the top line of the image, we need to
                    // scan the bottom line of the region to the north
                    // in order to get the correct lastY for shading
                    chunk = getChunkAt(nmsWorld, chunkX, chunkZ - 1);
                    if (chunk != null && !chunk.isEmpty()) {
                        lastY = scanBottomRow(chunk);
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
            chunk = getChunkAt(nmsWorld, chunkX, chunkZ - 1);
            if (chunk != null && !chunk.isEmpty()) {
                lastY = scanBottomRow(chunk);
            }

            chunk = getChunkAt(nmsWorld, chunkX, chunkZ);
            if (chunk != null && !chunk.isEmpty()) {
                scanChunk(image, lastY, chunk);
            }

            curChunks.incrementAndGet();
        }, this.executor);
    }

    private void scanChunk(Image image, int[] lastY, Chunk chunk) {
        final int blockX = chunk.getPos().getBlockX();
        final int blockZ = chunk.getPos().getBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (cancelled) return;
                image.setPixel(blockX + x, blockZ + z, scanBlock(chunk, x, z, lastY));
            }
        }
    }

    private int @NonNull [] scanBottomRow(final @NonNull Chunk chunk) {
        final int[] lastY = new int[16];
        for (int x = 0; x < 16; x++) {
            if (cancelled) return lastY;
            final BlockPosition.MutableBlockPosition pos = new BlockPosition.MutableBlockPosition();
            final int yDiff = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, x, 15) + 1;
            pos.setValues(chunk.getPos().getBlockX() + x, yDiff, chunk.getPos().getBlockZ() + 15);
            iterateDown(chunk, pos);
            lastY[x] = pos.getY();
        }
        return lastY;
    }

    private int scanBlock(net.minecraft.server.v1_16_R3.Chunk chunk, int imgX, int imgZ, int[] lastY) {
        int blockX = chunk.getPos().getBlockX() + imgX;
        int blockZ = chunk.getPos().getBlockZ() + imgZ;

        IBlockData state;
        final BlockPosition.MutableBlockPosition mutablePos = new BlockPosition.MutableBlockPosition();

        final int yDiff = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, imgX, imgZ) + 1;
        mutablePos.setValues(blockX, yDiff, blockZ);

        if (yDiff > 1) {
            state = iterateDown(chunk, mutablePos);
        } else {
            // no blocks found, show invisible/air
            return 0x00000000;
        }

        final int curY = mutablePos.getY();

        int color = Colors.getMapColor(state);

        if (this.biomeColors != null) {
            color = this.biomeColors.get().modifyColorFromBiome(color, chunk, mutablePos);
        }

        int odd = (imgX + imgZ & 1);

        final Pair<Integer, IBlockData> fluidPair = this.findDepthIfFluid(mutablePos, state, chunk);
        if (fluidPair != null) {
            final int fluidDepth = fluidPair.left();
            final IBlockData fluidState = fluidPair.right();
            return getFluidColor(fluidDepth, color, state, fluidState, odd);
        }

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
            } while (!state.isAir());
        }
        do {
            mutablePos.c(EnumDirection.DOWN);
            state = chunk.getType(mutablePos);
        } while ((state.isAir() || invisibleBlocks.contains(state.getBlock())) && mutablePos.getY() > 0);
        return state;
    }

    private @Nullable Pair<Integer, IBlockData> findDepthIfFluid(final @NonNull BlockPosition blockPos, final @NonNull IBlockData state, final @NonNull Chunk chunk) {
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

    private int getFluidColor(final int fluidCountY, int color, final @NonNull IBlockData state, final @NonNull IBlockData fluidState, final int odd) {
        final FluidType fluid = state.getFluid().getType();
        boolean shaded = false;
        if (fluid == FluidTypes.WATER || fluid == FluidTypes.FLOWING_WATER) {
            if (this.worldConfig.MAP_WATER_CHECKERBOARD) {
                color = applyDepthCheckerboard(fluidCountY, color, odd);
                shaded = true;
            }
            if (this.worldConfig.MAP_WATER_CLEAR) {
                if (!this.worldConfig.MAP_WATER_CHECKERBOARD) {
                    color = Colors.shade(color, 0.85F - (fluidCountY * 0.01F)); // darken water color
                }
                color = Colors.mix(color, Colors.getMapColor(fluidState), 0.20F / (fluidCountY / 2.0F)); // mix block color with water color
                shaded = true;
            }
        } else if (fluid == FluidTypes.LAVA || fluid == FluidTypes.FLOWING_LAVA) {
            if (this.worldConfig.MAP_LAVA_CHECKERBOARD) {
                color = applyDepthCheckerboard(fluidCountY, color, odd);
                shaded = true;
            }
        }
        return shaded ? color : Colors.removeAlpha(color);
    }

    private int applyDepthCheckerboard(final double fluidCountY, final int color, final double odd) {
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
}
