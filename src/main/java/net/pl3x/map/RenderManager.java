package net.pl3x.map;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.pl3x.map.task.FullRender;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class RenderManager {
    private static final Map<UUID, BukkitRunnable> renders = new HashMap<>();

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
            throw new RuntimeException("Render already running on " + world.getName());
        }
        FullRender render = new FullRender(world);
        renders.put(world.getUID(), render);
        render.runTaskAsynchronously(Pl3xMap.getInstance());
    }
}
