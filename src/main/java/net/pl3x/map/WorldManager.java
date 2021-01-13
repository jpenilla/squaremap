package net.pl3x.map;

import net.pl3x.map.configuration.WorldConfig;
import net.pl3x.map.data.MapWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WorldManager {

    private static final Map<UUID, MapWorld> worlds = new HashMap<>();

    public static @NonNull MapWorld getWorld(final @NonNull World world) {
        final MapWorld w = worlds.get(world.getUID());
        if (w != null) {
            return w;
        }
        final MapWorld mapWorld = MapWorld.forWorld(world);
        worlds.put(world.getUID(), mapWorld);
        return mapWorld;
    }

    public static void start() {
        Bukkit.getWorlds().forEach(world -> {
            WorldConfig config = WorldConfig.get(world);
            if (config.MAP_ENABLED) {
                getWorld(world);
            }
        });
    }

    public static void shutdown() {
        worlds.values().forEach(MapWorld::shutdown);
        worlds.clear();
    }

}
