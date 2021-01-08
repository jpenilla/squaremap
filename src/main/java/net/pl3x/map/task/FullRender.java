package net.pl3x.map.task;

import net.pl3x.map.Logger;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.util.FileUtil;
import org.bukkit.World;

import java.io.IOException;

public class FullRender extends AbstractRender {
    public FullRender(World world) {
        super(world, world.getSpawnLocation(), Integer.MAX_VALUE);
    }

    @Override
    public void run() {
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

        super.run();
    }
}
