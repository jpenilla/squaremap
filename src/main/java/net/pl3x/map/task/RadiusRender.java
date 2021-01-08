package net.pl3x.map.task;

import net.pl3x.map.Logger;
import net.pl3x.map.configuration.Lang;
import org.bukkit.Location;

public class RadiusRender extends AbstractRender {
    public RadiusRender(Location center, int radius) {
        super(center.getWorld(), center, radius);
    }

    @Override
    public void run() {
        Logger.info(Lang.LOG_STARTED_RADIUSRENDER
                .replace("{world}", world.getName()));
        super.run();
    }
}
