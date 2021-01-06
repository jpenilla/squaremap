package net.pl3x.map.task;

import java.io.IOException;
import java.util.List;
import net.pl3x.map.Logger;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.data.Region;
import net.pl3x.map.util.FileUtil;
import net.pl3x.map.util.SpiralIterator;
import org.bukkit.World;

public class FullRender extends AbstractRender {
    public FullRender(World world) {
        super(world);
    }

    @Override
    public void run() {
        super.run();
        Logger.info(Lang.LOG_STARTED_FULLRENDER
                .replace("{world}", world.getName()));

        try {
            FileUtil.deleteSubdirectories(worldTilesDir);
        } catch (IOException e) {
            Logger.severe(Lang.LOG_UNABLE_TO_WRITE_TO_FILE
                    .replace("{path}", worldTilesDir.toAbsolutePath().toString()));
            cancel();
            return;
        }

        Logger.info(Lang.LOG_SCANNING_REGION_FILES);
        List<Region> regions = getRegions();
        Logger.debug(Lang.LOG_FOUND_TOTAL_REGION_FILES
                .replace("{total}", Integer.toString(totalRegions)));

        SpiralIterator spiral = new SpiralIterator(0, 0, maxRadius + 1);
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
}
