package net.pl3x.map.plugin.task.render;

import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.data.MapWorld;
import net.pl3x.map.plugin.data.Region;
import net.pl3x.map.plugin.util.FileUtil;
import net.pl3x.map.plugin.util.Numbers;
import net.pl3x.map.plugin.util.iterator.RegionSpiralIterator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

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

        RegionSpiralIterator spiral, oldSpiral = this.mapWorld.getRenderProgress();
        if (oldSpiral != null) {
            Logger.info(Lang.LOG_RESUMED_RENDERING, Template.of("world", world.getName()));
        } else {
            Logger.info(Lang.LOG_STARTED_FULLRENDER, Template.of("world", world.getName()));
        }

        final List<Region> regions = getRegions();
        this.totalRegions = regions.size();
        Logger.info(Lang.LOG_FOUND_TOTAL_REGION_FILES, Template.of("total", Integer.toString(totalRegions)));

        this.totalChunks = totalRegions * 32 * 32;

        if (oldSpiral != null) {
            spiral = oldSpiral;
            this.curChunks.set(spiral.curStep() * 32 * 32);
        } else {
            Location spawn = world.getSpawnLocation();
            spiral = new RegionSpiralIterator(
                    Numbers.blockToRegion(spawn.getBlockX()),
                    Numbers.blockToRegion(spawn.getBlockZ()),
                    maxRadius);
        }

        final Timer timer = RenderProgress.printProgress(this);

        while (spiral.hasNext()) {
            if (this.cancelled) break;
            this.mapWorld.saveRenderProgress(spiral);
            Region region = spiral.next();
            if (regions.contains(region)) {
                mapRegion(region);
            }
            curRegions.incrementAndGet();
        }

        timer.cancel();

    }

    @Override
    public int totalChunks() {
        return this.totalChunks;
    }

    @Override
    public int totalRegions() {
        return this.totalRegions;
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
