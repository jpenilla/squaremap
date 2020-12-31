package net.pl3x.map.task;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.pl3x.map.Logger;
import net.pl3x.map.RenderManager;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.data.Image;
import net.pl3x.map.data.Region;
import net.pl3x.map.util.Colors;
import net.pl3x.map.util.FileUtil;
import net.pl3x.map.util.SpiralIterator;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class FullRender extends BukkitRunnable {
    private final World world;
    private final File worldTileDir;
    private final DecimalFormat df = new DecimalFormat("##0.00%");
    private int maxRadius = 0;

    private int total, current;

    public FullRender(World world) {
        this.world = world;
        this.worldTileDir = new File(new File(FileUtil.getWebFolder(), "tiles"), world.getName());
        if (!worldTileDir.exists() && !worldTileDir.mkdirs()) {
            Logger.severe(Lang.LOG_COULD_NOT_CREATE_DIR
                    .replace("{path}", worldTileDir.getAbsolutePath()));
        }
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

        if (!FileUtil.deleteDirectory(worldTileDir)) {
            Logger.severe(Lang.LOG_UNABLE_TO_WRITE_TO_FILE
                    .replace("{path}", worldTileDir.getAbsolutePath()));
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
        Logger.info(Lang.LOG_SCANNING_REGION_PROGRESS
                .replace("{progress}", progress())
                .replace("{x}", Integer.toString(region.getX()))
                .replace("{z}", Integer.toString(region.getZ())));
        Image image = new Image();
        int rendered = 0;
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                Chunk chunk = world.getChunkAtAsync(region.getChunkX() + x, region.getChunkZ() + z, false, true).join();
                if (chunk != null) {
                    rendered++;
                    mapChunk(chunk, image);
                }
            }
        }
        current++;
        if (rendered > 0) {
            Logger.debug(Lang.LOG_SAVING_CHUNKS_FOR_REGION
                    .replace("{total}", Integer.toString(rendered))
                    .replace("{x}", Integer.toString(region.getX()))
                    .replace("{z}", Integer.toString(region.getZ())));
            image.save(region, worldTileDir);
        } else {
            Logger.debug(Lang.LOG_SKIPPING_EMPTY_REGION
                    .replace("{x}", Integer.toString(region.getX()))
                    .replace("{z}", Integer.toString(region.getZ())));
        }
    }

    private void mapChunk(Chunk chunk, Image image) {
        int cX = chunk.getX() << 4;
        int cZ = chunk.getZ() << 4;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int bX = cX + x;
                int bZ = cZ + z;
                image.setPixel(bX, bZ, Colors.getColor(chunk, bX, bZ));
            }
        }
    }

    private String progress() {
        return String.format("%1$7s", df.format((double) current / total));
    }
}
