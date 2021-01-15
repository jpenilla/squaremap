package net.pl3x.map;

import net.pl3x.map.configuration.WorldConfig;
import net.pl3x.map.data.MapWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class WorldManager {

    private static final Map<UUID, MapWorld> worlds = new HashMap<>();

    public static @NonNull Map<UUID, MapWorld> worlds() {
        return Collections.unmodifiableMap(WorldManager.worlds);
    }

    public static @NonNull Optional<MapWorld> getWorldIfEnabled(final @NonNull World world) {
        if (WorldConfig.get(world).MAP_ENABLED) {
            return Optional.of(getWorld(world));
        } else {
            return Optional.empty();
        }
    }

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
