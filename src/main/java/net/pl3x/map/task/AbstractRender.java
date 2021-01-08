package net.pl3x.map.task;

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
import net.pl3x.map.Logger;
import net.pl3x.map.RenderManager;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.configuration.WorldConfig;
import net.pl3x.map.data.Image;
import net.pl3x.map.data.Region;
import net.pl3x.map.util.BiomeColors;
import net.pl3x.map.util.Colors;
import net.pl3x.map.util.FileUtil;
import net.pl3x.map.util.Numbers;
import net.pl3x.map.util.Pair;
import net.pl3x.map.util.SpecialColorRegistry;
import net.pl3x.map.util.SpiralIterator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRender extends BukkitRunnable {
    private static final Set<Block> invisibleBlocks = ImmutableSet.of(
            Blocks.TALL_GRASS,
            Blocks.FERN,
            Blocks.GRASS,
            Blocks.LARGE_FERN
    );

    protected final World world;
    private final WorldServer nmsWorld;
    private final WorldConfig worldConfig;
    protected final Path worldTilesDir;
    private final int centerX;
    private final int centerZ;
    private final int radius;

    private final DecimalFormat dfPercent = new DecimalFormat("0.00%");
    private final DecimalFormat dfRate = new DecimalFormat("0.0");
    private final BlockPosition.MutableBlockPosition pos1 = new BlockPosition.MutableBlockPosition();
    private final BlockPosition.MutableBlockPosition pos2 = new BlockPosition.MutableBlockPosition();
    private final BiomeColors biomeColors;

    protected int maxRadius = 0;
    protected int totalRegions, curRegions;
    private int curChunks, lastChunks, totalChunks;
    private long lastTime;
    protected boolean cancelled = false;

    public AbstractRender(World world, Location center, int radius) {
        this.world = world;
        this.nmsWorld = ((CraftWorld) world).getHandle();
        this.worldConfig = WorldConfig.get(world);
        this.worldTilesDir = FileUtil.getWorldFolder(world);
        this.centerX = Numbers.blockToRegion(center.getBlockX());
        this.centerZ = Numbers.blockToRegion(center.getBlockZ());
        this.radius = Numbers.blockToRegion(radius);
        this.biomeColors = this.worldConfig.MAP_BIOMES ? BiomeColors.forWorld(nmsWorld) : null; // We don't need to bother with initing this if we don't map biomes
    }

    @Override
    public void cancel() {
        RenderManager.finish(world);
        cancelled = true;
        super.cancel();
    }

    @Override
    public void run() {
        lastTime = System.currentTimeMillis();

        Logger.info(Lang.LOG_SCANNING_REGION_FILES);
        List<Region> regions = getRegions();

        maxRadius = Math.min(maxRadius, radius);

        Logger.info(Lang.LOG_FOUND_TOTAL_REGION_FILES
                .replace("{total}", Integer.toString(totalRegions)));

        SpiralIterator spiral = new SpiralIterator(centerX, centerZ, maxRadius);
        while (spiral.hasNext()) {
            if (cancelled) return;
            Region region = spiral.next();
            if (regions.contains(region)) {
                mapRegion(region);
            }
        }

        Logger.info(Lang.LOG_FINISHED_RENDERING
                .replace("{world}", world.getName()));

        cancel();
    }

    protected List<Region> getRegions() {
        int minX = centerX - radius;
        int maxX = centerX + radius;
        int minZ = centerZ - radius;
        int maxZ = centerZ + radius;
        List<Region> regions = new ArrayList<>();
        File[] files = FileUtil.getRegionFiles(world);
        for (File file : files) {
            if (cancelled) return new ArrayList<>();
            if (file.length() == 0) continue;
            try {
                String[] split = file.getName().split("\\.");
                int x = Integer.parseInt(split[1]);
                int z = Integer.parseInt(split[2]);
                if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                    Region region = new Region(x, z);
                    maxRadius = Math.max(Math.max(maxRadius, Math.abs(x)), Math.abs(z));
                    regions.add(region);
                }
            } catch (NumberFormatException ignore) {
            }
        }
        totalRegions = regions.size();
        totalChunks = totalRegions * 32 * 32;
        return regions;
    }

    protected void mapRegion(Region region) {
        Image image = new Image();
        int scanned = 0;
        int startX = region.getBlockX();
        int startZ = region.getBlockZ();
        for (int blockX = startX; blockX < startX + Image.SIZE; blockX += 16) {
            int[] lastY = new int[16];
            for (int blockZ = startZ; blockZ < startZ + Image.SIZE; blockZ += 16) {
                int chunkX = Numbers.blockToChunk(blockX);
                int chunkZ = Numbers.blockToChunk(blockZ);
                net.minecraft.server.v1_16_R3.Chunk chunk;
                if (blockZ == startZ) {
                    // this is the top line of the image, we need to
                    // scan the bottom line of the region to the north
                    // in order to get the correct lastY for shading
                    chunk = getChunkAt(nmsWorld, chunkX, chunkZ - 1);
                    if (chunk != null && !chunk.isEmpty()) {
                        for (int x = 0; x < 16; x++) {
                            if (cancelled) return;
                            scanBlock(chunk, x, 15, lastY);
                        }
                    }
                }
                chunk = getChunkAt(nmsWorld, chunkX, chunkZ);
                if (chunk != null && !chunk.isEmpty()) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            if (cancelled) return;
                            image.setPixel(blockX + x, blockZ + z, scanBlock(chunk, x, z, lastY));
                        }
                    }
                    scanned++;
                }
                curChunks++;
            }
        }
        curRegions++;
        printProgress(region);
        if (scanned > 0) {
            Logger.debug(Lang.LOG_SAVING_CHUNKS_FOR_REGION
                    .replace("{total}", Integer.toString(scanned))
                    .replace("{x}", Integer.toString(region.getX()))
                    .replace("{z}", Integer.toString(region.getZ())));
            image.save(region, worldTilesDir);
        } else {
            Logger.debug(Lang.LOG_SKIPPING_EMPTY_REGION
                    .replace("{x}", Integer.toString(region.getX()))
                    .replace("{z}", Integer.toString(region.getZ())));
        }
    }

    private int scanBlock(net.minecraft.server.v1_16_R3.Chunk chunk, int imgX, int imgZ, int[] lastY) {
        int curY = 0;
        int blockX = chunk.getPos().getBlockX() + imgX;
        int blockZ = chunk.getPos().getBlockZ() + imgZ;

        IBlockData state;

        if (nmsWorld.getDimensionManager().hasCeiling()) {
            final int yDiff = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, imgX, imgZ);
            pos1.setValues(blockX, yDiff, blockZ);

            do {
                pos1.c(EnumDirection.DOWN);
                state = chunk.getType(pos1);
            } while (!state.isAir());
            do {
                pos1.c(EnumDirection.DOWN);
                state = chunk.getType(pos1);
            } while ((state.isAir() || invisibleBlocks.contains(state.getBlock())) && pos1.getY() > 0);
        } else {
            final int yDiff = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, imgX, imgZ) + 1;
            pos1.setValues(blockX, yDiff, blockZ);

            if (yDiff > 1) {
                do {
                    pos1.c(EnumDirection.DOWN);
                    state = chunk.getType(pos1);
                } while ((state.isAir() || invisibleBlocks.contains(state.getBlock())) && pos1.getY() > 0);
            } else {
                state = Blocks.BEDROCK.getBlockData();
            }
        }

        curY += pos1.getY();

        int color = Colors.getMapColor(state).rgb;

        if (this.biomeColors != null) {
            color = this.biomeColors.modifyColorFromBiome(color, chunk, this.pos1);
        }

        int specialCase = SpecialColorRegistry.getColor(state);
        if (specialCase != -1) {
            color = specialCase;
        }

        double diffY;
        byte colorOffset;
        int odd = (imgX + imgZ & 1);

        final Pair<Integer, IBlockData> fluidPair = this.findDepthIfFluid(pos1.getY(), state, chunk);
        if (fluidPair != null) {
            final int fluidDepth = fluidPair.left();
            final IBlockData fluidState = fluidPair.right();
            return getFluidColor(fluidDepth, color, state, fluidState, odd);
        }

        diffY = ((double) curY - lastY[imgX]) * 4.0D / (double) 4 + ((double) odd - 0.5D) * 0.4D;
        colorOffset = (byte) (diffY > 0.6D ? 2 : (diffY < -0.6D ? 0 : 1));
        lastY[imgX] = curY;
        return Colors.shade(color, colorOffset);
    }

    private @Nullable Pair<Integer, IBlockData> findDepthIfFluid(final int surfaceY, final @NonNull IBlockData state, final @NonNull Chunk chunk) {
        if (surfaceY > 0 && !state.getFluid().isEmpty()) {
            IBlockData fluidState;
            int fluidDepth = 0;

            int yBelowSurface = surfaceY - 1;
            pos2.setValues(pos1);
            do {
                pos2.setY(yBelowSurface--);
                fluidState = chunk.getType(pos2);
                ++fluidDepth;
            } while (yBelowSurface > 0 && fluidDepth <= 10 && !fluidState.getFluid().isEmpty());

            return Pair.of(fluidDepth, fluidState);
        }
        return null;
    }

    private int getFluidColor(final int fluidCountY, int color, final @NonNull IBlockData state, final @NonNull IBlockData fluidState, final int odd) {
        final FluidType fluid = state.getFluid().getType();
        if (fluid == FluidTypes.WATER || fluid == FluidTypes.FLOWING_WATER) {
            if (this.worldConfig.MAP_WATER_CHECKERBOARD) {
                color = applyDepthCheckerboard(fluidCountY, color, odd);
            } else {
                color = (0xFF << 24) | (color & 0x00FFFFFF); // alpha seems to be missing from fluids :/
            }
            if (this.worldConfig.MAP_WATER_CLEAR) {
                color = Colors.shade(color, 0.85F - (fluidCountY * 0.01F)); // darken water color
                color = Colors.mix(color, Colors.getMapColor(fluidState).rgb, 0.20F / (fluidCountY / 2.0F)); // mix block color with water color
            }
        } else if (fluid == FluidTypes.LAVA || fluid == FluidTypes.FLOWING_LAVA) {
            if (this.worldConfig.MAP_LAVA_CHECKERBOARD) {
                color = applyDepthCheckerboard(fluidCountY, color, odd);
            } else {
                color = (0xFF << 24) | (color & 0x00FFFFFF); // alpha seems to be missing from fluids :/
            }
        }
        return color;
    }

    private int applyDepthCheckerboard(final double fluidCountY, int color, final double odd) {
        double diffY;
        byte colorOffset;
        diffY = fluidCountY * 0.1D + odd * 0.2D;
        colorOffset = (byte) (diffY < 0.5D ? 2 : (diffY > 0.9D ? 0 : 1));
        color = Colors.shade(color, colorOffset);
        return color;
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

    protected void printProgress(Region region) {
        long curTime = System.currentTimeMillis();
        long timeDiff = curTime - lastTime;
        lastTime = curTime;

        double chunks = curChunks - lastChunks;
        lastChunks = curChunks;

        double cpms = chunks / timeDiff;

        double chunksLeft = totalChunks - curChunks;
        long timeLeft = (long) (chunksLeft / cpms);

        int hrs = (int) TimeUnit.MILLISECONDS.toHours(timeLeft) % 24;
        int min = (int) TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60;
        int sec = (int) TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60;

        double percent = (double) curRegions / (double) totalRegions;

        String rateStr = dfRate.format(cpms * 1000);
        String percentStr = dfPercent.format(percent);
        String etaStr = String.format("%02d:%02d:%02d", hrs, min, sec);

        Logger.info(region == null ? Lang.LOG_SCANNING_REGIONS_FINISHED : Lang.LOG_SCANNING_REGIONS_PROGRESS
                .replace("{world}", world.getName())
                .replace("{chunks}", Integer.toString(curChunks))
                .replace("{percent}", percentStr)
                .replace("{eta}", etaStr)
                .replace("{rate}", rateStr)
                .replace("{x}", Integer.toString(region.getX()))
                .replace("{z}", Integer.toString(region.getZ())));
    }
}
