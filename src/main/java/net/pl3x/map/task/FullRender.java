package net.pl3x.map.task;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.ChunkProviderServer;
import net.minecraft.server.v1_16_R3.EnumDirection;
import net.minecraft.server.v1_16_R3.Fluid;
import net.minecraft.server.v1_16_R3.HeightMap;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.MaterialMapColor;
import net.minecraft.server.v1_16_R3.WorldServer;
import net.pl3x.map.Logger;
import net.pl3x.map.RenderManager;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.data.Image;
import net.pl3x.map.data.Region;
import net.pl3x.map.util.FileUtil;
import net.pl3x.map.util.SpiralIterator;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.scheduler.BukkitRunnable;

public class FullRender extends BukkitRunnable {
    private static final MaterialMapColor BLACK = MaterialMapColor.b;
    private static final MaterialMapColor BLUE = MaterialMapColor.n;

    private final World world;
    private final WorldServer nmsWorld;
    private final File worldTileDir;
    private final DecimalFormat df = new DecimalFormat("##0.00%");
    private final BlockPosition.MutableBlockPosition pos1 = new BlockPosition.MutableBlockPosition();
    private final BlockPosition.MutableBlockPosition pos2 = new BlockPosition.MutableBlockPosition();
    private final Multiset<MaterialMapColor> multiset = LinkedHashMultiset.create();

    private int maxRadius = 0;
    private int total, current;

    public FullRender(World world) {
        this.world = world;
        this.nmsWorld = ((CraftWorld) world).getHandle();
        this.worldTileDir = FileUtil.getWorldFolder(world);
    }

    @Override
    public void cancel() {
        RenderManager.finish(world);
        super.cancel();
    }

    @Override
    public void run() {
        Logger.info(Lang.LOG_STARTED_FULLRENDER
                .replace("{world}", world.getName()));

        if (!FileUtil.deleteSubDirs(worldTileDir)) {
            Logger.severe(Lang.LOG_UNABLE_TO_WRITE_TO_FILE
                    .replace("{path}", worldTileDir.getAbsolutePath()));
            cancel();
            return;
        }

        Logger.info(Lang.LOG_SCANNING_REGION_FILES);
        List<Region> regions = getRegions();
        total = regions.size();
        Logger.debug(Lang.LOG_FOUND_TOTAL_REGION_FILES
                .replace("{total}", Integer.toString(total)));

        SpiralIterator spiral = new SpiralIterator(0, 0, maxRadius + 1);
        while (spiral.hasNext()) {
            Region region = spiral.next();
            if (regions.contains(region)) {
                mapRegion(region);
            }
        }
        Logger.info(Lang.LOG_SCANNING_REGIONS_FINISHED
                .replace("{progress}", progress()));

        Logger.info(Lang.LOG_FINISHED_RENDERING
                .replace("{world}", world.getName()));
        cancel();
    }

    private List<Region> getRegions() {
        List<Region> regions = new ArrayList<>();
        File[] files = FileUtil.getRegionFiles(world);
        for (File file : files) {
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
        return regions;
    }

    private void mapRegion(Region region) {
        Logger.info(Lang.LOG_SCANNING_REGIONS_PROGRESS
                .replace("{progress}", progress())
                .replace("{x}", Integer.toString(region.getX()))
                .replace("{z}", Integer.toString(region.getZ())));
        Image image = new Image();
        int scanned = 0;
        int startX = region.getBlockX();
        int startZ = region.getBlockZ();
        for (int blockX = startX; blockX < startX + Image.SIZE; blockX += 16) {
            double[] lastY = new double[16];
            for (int blockZ = startZ; blockZ < startZ + Image.SIZE; blockZ += 16) {
                net.minecraft.server.v1_16_R3.Chunk chunk;
                if (blockZ == startZ) {
                    // this is the top line of the image, we need to
                    // scan the bottom line of the region to the north
                    // in order to get the correct lastY for shading
                    chunk = getChunkAt(nmsWorld, blockX >> 4, (blockZ >> 4) - 1);
                    if (chunk != null && !chunk.isEmpty()) {
                        for (int x = 0; x < 16; x++) {
                            scanBlock(chunk, x, 15, lastY);
                        }
                    }
                }
                chunk = getChunkAt(nmsWorld, blockX >> 4, blockZ >> 4);
                if (chunk != null && !chunk.isEmpty()) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            image.setPixel(blockX + x, blockZ + z, scanBlock(chunk, x, z, lastY));
                        }
                    }
                }
                scanned++;
            }
        }
        current++;
        if (scanned > 0) {
            Logger.debug(Lang.LOG_SAVING_CHUNKS_FOR_REGION
                    .replace("{total}", Integer.toString(scanned))
                    .replace("{x}", Integer.toString(region.getX()))
                    .replace("{z}", Integer.toString(region.getZ())));
            image.save(region, worldTileDir);
        } else {
            Logger.debug(Lang.LOG_SKIPPING_EMPTY_REGION
                    .replace("{x}", Integer.toString(region.getX()))
                    .replace("{z}", Integer.toString(region.getZ())));
        }
    }

    private int scanBlock(net.minecraft.server.v1_16_R3.Chunk chunk, int imgX, int imgZ, double[] lastY) {
        int fluidCountY = 0;
        double curY = 0.0D;
        int odd = (imgX + imgZ & 1);
        multiset.clear();
        if (nmsWorld.getDimensionManager().hasCeiling()) {
            // TODO figure out how to actually map the nether instead of this crap
            int l3 = (chunk.getPos().getBlockX() + imgX) + (chunk.getPos().getBlockZ() + imgZ) * 231871;
            l3 = l3 * l3 * 31287121 + l3 * 11;
            if ((l3 >> 20 & 1) == 0) {
                multiset.add(getColor(Blocks.DIRT.getBlockData()), 10);
            } else {
                multiset.add(getColor(Blocks.STONE.getBlockData()), 100);
            }
            curY = 100.0D;
        } else {
            for (int stepX = 0; stepX < 1; ++stepX) {
                for (int stepZ = 0; stepZ < 1; ++stepZ) {
                    int yDiffFromSurface = chunk.getHighestBlock(HeightMap.Type.WORLD_SURFACE, stepX + imgX, stepZ + imgZ) + 1;
                    IBlockData state;
                    if (yDiffFromSurface > 1) {
                        do {
                            --yDiffFromSurface;
                            pos1.setValues(chunk.getPos().getBlockX() + imgX + stepX, yDiffFromSurface, chunk.getPos().getBlockZ() + imgZ + stepX);
                            state = chunk.getType(pos1);
                        } while (getColor(state) == BLACK && yDiffFromSurface > 0);
                        if (yDiffFromSurface > 0 && !state.getFluid().isEmpty()) {
                            int yBelowSurface = yDiffFromSurface - 1;
                            pos2.setValues(pos1);
                            IBlockData fluidState;
                            do {
                                pos2.setY(yBelowSurface--);
                                fluidState = chunk.getType(pos2);
                                ++fluidCountY;
                            } while (yBelowSurface > 0 && !fluidState.getFluid().isEmpty());
                            state = getFluidStateIfVisible(chunk.world, state, pos1);
                        }
                    } else {
                        state = Blocks.BEDROCK.getBlockData();
                    }
                    curY += yDiffFromSurface;
                    multiset.add(getColor(state));
                }
            }
        }

        //noinspection UnstableApiUsage
        MaterialMapColor color = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), BLACK);
        double diffY = (curY - lastY[imgX]) * 4.0D / (double) 4 + ((double) odd - 0.5D) * 0.4D;
        byte colorOffset = (byte) (diffY > 0.6D ? 2 : (diffY < -0.6D ? 0 : 1));
        lastY[imgX] = curY;

        if (color == null) {
            color = BLACK;
        } else if (color == BLUE) {
            diffY = (double) fluidCountY * 0.1D + (double) odd * 0.2D;
            colorOffset = (byte) (diffY < 0.5D ? 2 : (diffY > 0.9D ? 0 : 1));
        }

        return getRenderColor(color.rgb, colorOffset);
    }

    private MaterialMapColor getColor(IBlockData state) {
        return state.d(null, null);
    }

    private IBlockData getFluidStateIfVisible(net.minecraft.server.v1_16_R3.World world, IBlockData state, BlockPosition pos) {
        Fluid fluid = state.getFluid();
        return !fluid.isEmpty() && !state.d(world, pos, EnumDirection.UP) ? fluid.getBlockData() : state;
    }

    private int getRenderColor(int color, int shade) {
        float ratio = 220F / 255F;
        if (shade == 2) ratio = 1.0F;
        if (shade == 0) ratio = 180F / 255F;
        int r = (int) ((color >> 16 & 0xFF) * (ratio));
        int g = (int) ((color >> 8 & 0xFF) * (ratio));
        int b = (int) ((color & 0xFF) * (ratio));
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private net.minecraft.server.v1_16_R3.Chunk getChunkAt(net.minecraft.server.v1_16_R3.World world, int x, int z) {
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        net.minecraft.server.v1_16_R3.Chunk ifLoaded = provider.getChunkAtIfLoadedImmediately(x, z);
        if (ifLoaded != null) {
            return ifLoaded;
        }
        return (net.minecraft.server.v1_16_R3.Chunk) provider.getChunkAtAsynchronously(x, z, false, true).join().left().orElse(null);
    }

    private String progress() {
        return String.format("%1$7s", df.format((double) current / total));
    }
}
