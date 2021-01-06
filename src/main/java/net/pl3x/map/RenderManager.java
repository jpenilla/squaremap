package net.pl3x.map;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.task.FullRender;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class RenderManager {
    private static final Map<UUID, BukkitRunnable> renders = new HashMap<>();

    public static void stop() {
        Collection<BukkitRunnable> list = renders.values();
        for (BukkitRunnable render : list) {
            System.out.println("cancelled runnable");
            render.cancel();
        }
    }

    public static void finish(World world) {
        BukkitRunnable render = renders.remove(world.getUID());
        if (render != null && !render.isCancelled()) {
            render.cancel();
        }
    }

    public static boolean isRendering(World world) {
        return renders.containsKey(world.getUID());
    }

    public static void fullRender(World world) {
        if (isRendering(world)) {
            throw new RuntimeException(Lang.RENDER_IN_PROGRESS
                    .replace("{world}", world.getName()));
        }
        FullRender render = new FullRender(world);
        renders.put(world.getUID(), render);
        render.runTaskAsynchronously(Pl3xMap.getInstance());
    }

    public static void radiusRender(Location center, int radius) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
