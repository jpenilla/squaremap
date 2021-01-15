package net.pl3x.map.task.render;

import net.pl3x.map.Logger;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.data.MapWorld;
import net.pl3x.map.data.Region;
import net.pl3x.map.util.FileUtil;
import net.pl3x.map.util.iterator.RegionSpiralIterator;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public final class FullRender extends AbstractRender {
    public FullRender(final @NonNull MapWorld world) {
        super(world);
    }

    private int maxRadius = 0;
    private int totalRegions = 0;
    private int totalChunks;

    @Override
    protected void render() {

        final List<Region> regions = getRegions();
        this.totalRegions = regions.size();
        this.totalChunks = this.totalRegions * 32 * 32;
        Logger.info(Lang.LOG_FOUND_TOTAL_REGION_FILES
                .replace("{total}", Integer.toString(this.totalRegions)));

        final Timer timer = RenderProgress.printProgress(this);

        RegionSpiralIterator spiral = new RegionSpiralIterator(0, 0, maxRadius);
        while (spiral.hasNext()) {
            if (this.cancelled) break;
            Region region = spiral.next();
            if (regions.contains(region)) {
                mapRegion(region);
            }
        }

        timer.cancel();

        Logger.info(Lang.LOG_FINISHED_RENDERING
                .replace("{world}", world.getName()));

    }

    @Override
    public int totalChunks() {
        return this.totalChunks;
    }

    private List<Region> getRegions() {
        Logger.info(Lang.LOG_SCANNING_REGION_FILES);
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
}
