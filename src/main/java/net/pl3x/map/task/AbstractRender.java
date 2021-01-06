package net.pl3x.map.task;

import com.mojang.datafixers.util.Either;
import java.io.File;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.v1_16_R3.BiomeBase;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkProviderServer;
import net.minecraft.server.v1_16_R3.EnumDirection;
import net.minecraft.server.v1_16_R3.HeightMap;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.IChunkAccess;
import net.minecraft.server.v1_16_R3.Material;
import net.minecraft.server.v1_16_R3.MaterialMapColor;
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
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractRender extends BukkitRunnable {
    protected final World world;
    private final WorldServer nmsWorld;
    private final WorldConfig worldConfig;
    protected final Path worldTilesDir;
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

    public AbstractRender(World world) {
        this.world = world;
        this.nmsWorld = ((CraftWorld) world).getHandle();
        this.worldConfig = WorldConfig.get(world);
        this.worldTilesDir = FileUtil.getWorldFolder(world);
        this.biomeColors = new BiomeColors(this.nmsWorld);
    }

    @Override
    public void cancel() {
        RenderManager.finish(world);
        cancelled = true;
        super.cancel();
    }

    @Override
    public void run() {
        this.lastTime = System.currentTimeMillis();
    }

    protected List<Region> getRegions() {
        List<Region> regions = new ArrayList<>();
        File[] files = FileUtil.getRegionFiles(world);
        for (File file : files) {
            if (cancelled) return new ArrayList<>();
            if (file.length() == 0) continue;
            try {
                String[] split = file.getName().split("\\.");
                int x = Integer.parseInt(split[1]);
                int z = Integer.parseInt(split[2]);
                Region region = new Region(x, z);
                maxRadius = Math.max(Math.max(maxRadius, Math.abs(x)), Math.abs(z));
                regions.add(region);
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
                net.minecraft.server.v1_16_R3.Chunk chunk;
                if (blockZ == startZ) {
                    // this is the top line of the image, we need to
                    // scan the bottom line of the region to the north
                    // in order to get the correct lastY for shading
                    chunk = getChunkAt(nmsWorld, blockX >> 4, (blockZ >> 4) - 1);
                    if (chunk != null && !chunk.isEmpty()) {
                        for (int x = 0; x < 16; x++) {
                            if (cancelled) return;
                            scanBlock(chunk, x, 15, lastY);
                        }
                    }
                }
                chunk = getChunkAt(nmsWorld, blockX >> 4, blockZ >> 4);
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
        int fluidCountY = 0;
        int curY = 0;
        int odd = (imgX + imgZ & 1);
        int blockX = chunk.getPos().getBlockX() + imgX;
        int blockZ = chunk.getPos().getBlockZ() + imgZ;

        int color;
        IBlockData blockState;

        if (nmsWorld.getDimensionManager().hasCeiling()) {
            int yDiff = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, imgX, imgZ);
            pos1.setValues(blockX, yDiff, blockZ);

            do {
                pos1.c(EnumDirection.DOWN);
            } while (!chunk.getType(pos1).isAir());
            do {
                pos1.c(EnumDirection.DOWN);
            } while (chunk.getType(pos1).isAir() && pos1.getY() > 0);

            IBlockData state = chunk.getType(pos1);

            if (pos1.getY() > 0 && !state.getFluid().isEmpty()) {
                int yBelowSurface = pos1.getY() - 1;
                pos2.setValues(pos1);
                IBlockData fluidState;
                do {
                    pos2.setY(yBelowSurface--);
                    fluidState = chunk.getType(pos2);
                    ++fluidCountY;
                } while (yBelowSurface > 0 && !fluidState.getFluid().isEmpty());
            }
            curY += pos1.getY();

            blockState = state;
            color = getMapColor(state).rgb;
        } else {
            int yDiffFromSurface = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, imgX, imgZ) + 1;
            IBlockData state;
            if (yDiffFromSurface > 1) {
                do {
                    --yDiffFromSurface;
                    pos1.setValues(blockX, yDiffFromSurface, blockZ);
                    state = chunk.getType(pos1);
                } while (getMapColor(state) == Colors.blackMapColor() && yDiffFromSurface > 0);

                if (yDiffFromSurface > 0 && !state.getFluid().isEmpty()) {
                    int yBelowSurface = yDiffFromSurface - 1;
                    pos2.setValues(pos1);
                    IBlockData fluidState;
                    do {
                        pos2.setY(yBelowSurface--);
                        fluidState = chunk.getType(pos2);
                        ++fluidCountY;
                    } while (yBelowSurface > 0 && !fluidState.getFluid().isEmpty());
                }
            } else {
                state = Blocks.BEDROCK.getBlockData();
            }
            curY += yDiffFromSurface;

            blockState = state;
            color = getMapColor(state).rgb;
        }

        if (worldConfig.MAP_BIOMES) {
            BiomeBase biome = null;
            if (worldConfig.MAP_BLEND_BIOMES == 0) {
                biome = nmsWorld.getBiome(pos1);
            }

            final IBlockData data = chunk.getType(pos1);
            final Material mat = data.getMaterial();
            final Block block = data.getBlock();

            if (mat == Material.GRASS) {
                color = worldConfig.MAP_BLEND_BIOMES > 0 ? this.biomeColors.smoothGrass(pos1) : this.biomeColors.grass(biome, pos1);
            } else if (mat == Material.REPLACEABLE_PLANT || mat == Material.PLANT) {
                // todo: properly render tall grass/plant color (vanilla maps render them the same color as leaves, but in game they are actually the same color as grass blocks)
                color = Colors.shade(worldConfig.MAP_BLEND_BIOMES > 0 ? this.biomeColors.smoothGrass(pos1) : this.biomeColors.grass(biome, pos1), 1);
                //color = Colors.shade(this.biomeColors.grass(biome, pos1), 1);
            } else if (mat == Material.LEAVES || block == Blocks.VINE) {
                if (block != Blocks.BIRCH_LEAVES && block != Blocks.SPRUCE_LEAVES) {
                    color = worldConfig.MAP_BLEND_BIOMES > 0 ? this.biomeColors.smoothFoliage(pos1) : this.biomeColors.foliage(biome, pos1);
                    //color = this.biomeColors.foliage(biome, pos1);
                }
            } else if (worldConfig.MAP_WATER_BIOMES &&
                    (mat == Material.WATER || mat == Material.WATER_PLANT || mat == Material.REPLACEABLE_WATER_PLANT)) {
                color = worldConfig.MAP_BLEND_BIOMES > 0 ? this.biomeColors.smoothWater(pos1) : this.biomeColors.water(biome, pos1);
                //color = this.biomeColors.water(biome, pos1);
            }
        }

        double diffY;
        byte colorOffset;
        if (!blockState.getFluid().isEmpty()) {
            diffY = (double) fluidCountY * 0.1D + (double) odd * 0.2D;
            colorOffset = (byte) (diffY < 0.5D ? 2 : (diffY > 0.9D ? 0 : 1));
        } else {
            diffY = ((double) curY - lastY[imgX]) * 4.0D / (double) 4 + ((double) odd - 0.5D) * 0.4D;
            colorOffset = (byte) (diffY > 0.6D ? 2 : (diffY < -0.6D ? 0 : 1));
            lastY[imgX] = curY;
        }

        return Colors.shade(color, colorOffset);
    }

    private @NonNull MaterialMapColor getMapColor(IBlockData state) {
        return state.d(null, null);
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
        this.lastTime = curTime;

        double chunks = curChunks - lastChunks;
        this.lastChunks = curChunks;

        double rate = chunks / TimeUnit.MILLISECONDS.toSeconds(timeDiff);

        double chunksLeft = totalChunks - curChunks;
        int timeLeft = (int) (chunksLeft / rate);

        double percent = (double) curRegions / (double) totalRegions;

        int hrs = (int) TimeUnit.SECONDS.toHours(timeLeft) % 24;
        int min = (int) TimeUnit.SECONDS.toMinutes(timeLeft) % 60;
        int sec = (int) TimeUnit.SECONDS.toSeconds(timeLeft) % 60;

        String rateStr = dfRate.format(rate);
        String percentStr = dfPercent.format(percent);
        String etaStr = String.format("%02d:%02d:%02d", hrs, min, sec);

        Logger.info(region == null ? Lang.LOG_SCANNING_REGIONS_FINISHED : Lang.LOG_SCANNING_REGIONS_PROGRESS
                .replace("{chunks}", Integer.toString(curChunks))
                .replace("{percent}", percentStr)
                .replace("{eta}", etaStr)
                .replace("{rate}", rateStr)
                .replace("{x}", Integer.toString(region.getX()))
                .replace("{z}", Integer.toString(region.getZ())));
    }
}
