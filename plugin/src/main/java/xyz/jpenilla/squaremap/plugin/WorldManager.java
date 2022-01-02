package xyz.jpenilla.squaremap.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.plugin.config.WorldConfig;
import xyz.jpenilla.squaremap.plugin.data.MapWorld;

public final class WorldManager {
    private final Map<WorldIdentifier, MapWorld> worlds = new ConcurrentHashMap<>();

    public @NonNull Map<WorldIdentifier, MapWorld> worlds() {
        return Collections.unmodifiableMap(this.worlds);
    }

    public @NonNull Optional<MapWorld> getWorldIfEnabled(final @NonNull World world) {
        if (WorldConfig.get(world).MAP_ENABLED) {
            return Optional.of(this.getWorld(world));
        } else {
            return Optional.empty();
        }
    }

    public @NonNull MapWorld getWorld(final @NonNull World world) {
        return this.worlds.computeIfAbsent(BukkitAdapter.worldIdentifier(world), $ -> MapWorld.forWorld(world));
    }

    public void start() {
        Bukkit.getWorlds().forEach(world -> {
            WorldConfig config = WorldConfig.get(world);
            if (config.MAP_ENABLED) {
                this.getWorld(world);
            }
        });
    }

    public void worldUnloaded(final @NonNull World world) {
        this.getWorldIfEnabled(world).ifPresent(MapWorld::shutdown);
        this.worlds.remove(BukkitAdapter.worldIdentifier(world));
    }

    public void shutdown() {
        this.worlds.values().forEach(MapWorld::shutdown);
        this.worlds.clear();
    }
}
