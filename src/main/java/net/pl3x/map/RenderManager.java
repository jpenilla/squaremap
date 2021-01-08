package net.pl3x.map;

import net.pl3x.map.configuration.Lang;
import net.pl3x.map.task.AbstractRender;
import net.pl3x.map.task.FullRender;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RenderManager {
    private static final Map<UUID, AbstractRender> renders = new HashMap<>();

    public static void stop() {
        Collection<AbstractRender> list = renders.values();
        for (AbstractRender render : list) {
            System.out.println("cancelled runnable");
            render.cancel();
        }
    }

    public static void finish(World world) {
        AbstractRender render = renders.remove(world.getUID());
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
        AbstractRender render = new FullRender(world);
        renders.put(world.getUID(), render);
        render.runTaskAsynchronously(Pl3xMap.getInstance());
    }

    public static void radiusRender(World world, Location center, int radius) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static void cancelRender(World world) {
        AbstractRender render = renders.get(world.getUID());
        if (render == null) {
            throw new RuntimeException(Lang.RENDER_NOT_IN_PROGRESS
                    .replace("{world}", world.getName()));
        }
        render.cancel();
    }
}
