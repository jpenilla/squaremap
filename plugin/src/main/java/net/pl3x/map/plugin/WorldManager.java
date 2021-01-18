package net.pl3x.map.plugin;

import net.pl3x.map.plugin.configuration.WorldConfig;
import net.pl3x.map.plugin.data.MapWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class WorldManager {

    private final Map<UUID, MapWorld> worlds = new HashMap<>();

    public @NonNull Map<UUID, MapWorld> worlds() {
        return Collections.unmodifiableMap(this.worlds);
    }

    public @NonNull Optional<MapWorld> getWorldIfEnabled(final @NonNull World world) {
        if (WorldConfig.get(world).MAP_ENABLED) {
            return Optional.of(getWorld(world));
        } else {
            return Optional.empty();
        }
    }

    public @NonNull MapWorld getWorld(final @NonNull World world) {
        final MapWorld w = this.worlds.get(world.getUID());
        if (w != null) {
            return w;
        }
        final MapWorld mapWorld = MapWorld.forWorld(world);
        this.worlds.put(world.getUID(), mapWorld);
        return mapWorld;
    }

    public void start() {
        Bukkit.getWorlds().forEach(world -> {
            WorldConfig config = WorldConfig.get(world);
            if (config.MAP_ENABLED) {
                getWorld(world);
            }
        });
    }

    public void shutdown() {
        this.worlds.values().forEach(MapWorld::shutdown);
        this.worlds.clear();
    }

}
