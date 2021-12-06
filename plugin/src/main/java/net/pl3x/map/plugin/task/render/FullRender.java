package net.pl3x.map.plugin.task.render;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Logging;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.data.MapWorld;
import net.pl3x.map.plugin.data.RegionCoordinate;
import net.pl3x.map.plugin.util.FileUtil;
import net.pl3x.map.plugin.util.Numbers;
import net.pl3x.map.plugin.util.iterator.RegionSpiralIterator;
import net.pl3x.map.plugin.visibilitylimit.VisibilityLimit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class FullRender extends AbstractRender {
    private int maxRadius = 0;
    private int totalChunks;
    private int totalRegions;

    public FullRender(final @NonNull MapWorld world) {
        super(world);
    }

    @Override
    protected void render() {
        while (Bukkit.getCurrentTick() < 20) {
            // server is not running yet
            sleep(1000);
        }

        // order preserved map of regions with boolean to signify if it was already scanned
        final Map<RegionCoordinate, Boolean> regions;

        Map<RegionCoordinate, Boolean> resumedMap = mapWorld.getRenderProgress();
        if (resumedMap != null) {
            Logging.info(Lang.LOG_RESUMED_RENDERING, Template.template("world", world.getName()));

            regions = resumedMap;

            final int count = (int) regions.values().stream().filter(bool -> bool).count();
            this.curRegions.set(count);
            this.curChunks.set(this.countCompletedChunks(regions));
        } else {
            Logging.info(Lang.LOG_STARTED_FULLRENDER, Template.template("world", world.getName()));

            // find all region files
            Logging.info(Lang.LOG_SCANNING_REGION_FILES);
            final List<RegionCoordinate> regionFiles = this.getRegions();

            // setup a spiral iterator
            Location spawn = world.getSpawnLocation();
            RegionSpiralIterator spiral = new RegionSpiralIterator(
                    Numbers.blockToRegion(spawn.getBlockX()),
                    Numbers.blockToRegion(spawn.getBlockZ()),
                    this.maxRadius);

            // iterate the spiral to get all regions needed
            int failsafe = 0;
            regions = new LinkedHashMap<>();
            while (spiral.hasNext()) {
                if (this.cancelled) break;
                if (failsafe > 500000) {
                    // we scanned over half a million non-existent regions straight
                    // quit the prescan and add the remaining regions to the end
                    regionFiles.forEach(region -> regions.put(region, false));
                    break;
                }
                RegionCoordinate region = spiral.next();
                if (regionFiles.contains(region)) {
                    regions.put(region, false);
                    failsafe = 0;
                } else {
                    failsafe++;
                }
            }
        }

        // ensure task wasnt cancelled before we start
        if (this.cancelled) return;

        VisibilityLimit visibility = this.mapWorld.visibilityLimit();
        this.totalRegions = regions.size();
        this.totalChunks = regions.keySet().stream().mapToInt(visibility::countChunksInRegion).sum();

        Logging.info(Lang.LOG_FOUND_TOTAL_REGION_FILES, Template.template("total", Integer.toString(regions.size())));

        this.timer = RenderProgress.printProgress(this);

        // finally, scan each region in the order provided by the spiral
        for (Map.Entry<RegionCoordinate, Boolean> entry : regions.entrySet()) {
            if (this.cancelled) break;
            if (entry.getValue()) continue;
            this.mapRegion(entry.getKey());
            entry.setValue(true);
            this.curRegions.incrementAndGet();
            // only save progress is task is not cancelled
            if (!this.cancelled) mapWorld.saveRenderProgress(regions);
        }

        if (this.timer != null) {
            this.timer.cancel();
        }

    }

    private int countCompletedChunks(final Map<RegionCoordinate, Boolean> regions) {
        final VisibilityLimit visibility = this.mapWorld.visibilityLimit();
        return regions.entrySet().stream()
                .filter(Map.Entry::getValue)
                .mapToInt(entry -> visibility.countChunksInRegion(entry.getKey()))
                .sum();
    }

    @Override
    public int totalChunks() {
        return this.totalChunks;
    }

    @Override
    public int totalRegions() {
        return this.totalRegions;
    }

    private List<RegionCoordinate> getRegions() {
        List<RegionCoordinate> regions = new ArrayList<>();
        File[] files = FileUtil.getRegionFiles(world);
        for (File file : files) {
            if (file.length() == 0) continue;
            try {
                String[] split = file.getName().split("\\.");
                int x = Integer.parseInt(split[1]);
                int z = Integer.parseInt(split[2]);

                RegionCoordinate region = new RegionCoordinate(x, z);

                // ignore regions completely outside the visibility limit
                if (!mapWorld.visibilityLimit().shouldRenderRegion(region)) {
                    continue;
                }

                this.maxRadius = Math.max(Math.max(maxRadius, Math.abs(x)), Math.abs(z));
                regions.add(region);

            } catch (NumberFormatException ignore) {
            }
        }

        return regions;
    }
}
