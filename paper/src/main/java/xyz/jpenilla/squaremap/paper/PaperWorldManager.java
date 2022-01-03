package xyz.jpenilla.squaremap.paper;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.WorldManager;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.paper.data.PaperMapWorld;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

public final class PaperWorldManager implements WorldManager {
    private final Map<WorldIdentifier, PaperMapWorld> worlds = new ConcurrentHashMap<>();

    @Override
    public @NonNull Map<WorldIdentifier, MapWorldInternal> worlds() {
        return Collections.unmodifiableMap(this.worlds);
    }

    @Override
    public @NonNull Optional<MapWorldInternal> getWorldIfEnabled(final @NonNull ServerLevel level) {
        return this.getWorldIfEnabled(level.getWorld()).map(Function.identity());
    }

    public @NonNull Optional<PaperMapWorld> getWorldIfEnabled(final @NonNull World world) {
        if (WorldConfig.get(CraftBukkitReflection.serverLevel(world)).MAP_ENABLED) {
            return Optional.of(this.getWorld(world));
        } else {
            return Optional.empty();
        }
    }

    public @NonNull PaperMapWorld getWorld(final @NonNull World world) {
        return this.worlds.computeIfAbsent(BukkitAdapter.worldIdentifier(world), $ -> PaperMapWorld.forWorld(world));
    }

    public void start() {
        Bukkit.getWorlds().forEach(world -> {
            WorldConfig config = WorldConfig.get(CraftBukkitReflection.serverLevel(world));
            if (config.MAP_ENABLED) {
                this.getWorld(world);
            }
        });
    }

    public void worldUnloaded(final @NonNull World world) {
        this.getWorldIfEnabled(world).ifPresent(PaperMapWorld::shutdown);
        this.worlds.remove(BukkitAdapter.worldIdentifier(world));
    }

    public void shutdown() {
        this.worlds.values().forEach(PaperMapWorld::shutdown);
        this.worlds.clear();
    }
}
