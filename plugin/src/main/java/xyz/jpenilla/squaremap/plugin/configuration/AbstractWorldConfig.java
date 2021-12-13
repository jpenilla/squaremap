package xyz.jpenilla.squaremap.plugin.configuration;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.WorldManager;
import xyz.jpenilla.squaremap.plugin.data.MapWorld;
import xyz.jpenilla.squaremap.plugin.util.ReflectionUtil;

@SuppressWarnings("unused")
abstract class AbstractWorldConfig {
    final ServerLevel world;
    final String worldName;
    final AbstractConfig config;

    AbstractWorldConfig(final World world, final AbstractConfig parent) {
        this.world = ReflectionUtil.CraftBukkit.serverLevel(world);
        this.worldName = world.getName();
        this.config = parent;
    }

    void set(final String path, final Object val) {
        this.config.yaml.addDefault("world-settings.default." + path, val);
        this.config.yaml.set("world-settings.default." + path, val);
        if (this.config.yaml.get("world-settings." + this.worldName + "." + path) != null) {
            this.config.yaml.addDefault("world-settings." + this.worldName + "." + path, val);
            this.config.yaml.set("world-settings." + this.worldName + "." + path, val);
        }
    }

    boolean getBoolean(final String path, final boolean def) {
        this.config.yaml.addDefault("world-settings.default." + path, def);
        return this.config.yaml.getBoolean("world-settings." + this.worldName + "." + path,
            this.config.yaml.getBoolean("world-settings.default." + path));
    }

    int getInt(final String path, final int def) {
        this.config.yaml.addDefault("world-settings.default." + path, def);
        return this.config.yaml.getInt("world-settings." + this.worldName + "." + path,
            this.config.yaml.getInt("world-settings.default." + path));
    }

    String getString(final String path, final String def) {
        this.config.yaml.addDefault("world-settings.default." + path, def);
        return this.config.yaml.getString("world-settings." + this.worldName + "." + path,
            this.config.yaml.getString("world-settings.default." + path));
    }

    <T> List<?> getList(final String path, final T def) {
        this.config.yaml.addDefault("world-settings.default." + path, def);
        return this.config.yaml.getList("world-settings." + this.worldName + "." + path,
            this.config.yaml.getList("world-settings.default." + path));
    }

    @NonNull <T> Map<String, T> getMap(final @NonNull String path, final @Nullable Map<String, T> def) {
        final Map<String, T> fallback = this.config.getMap("world-settings.default." + path, def);
        final Map<String, T> value = this.config.getMap("world-settings." + this.worldName + "." + path, null);
        return value.isEmpty() ? fallback : value;
    }

    protected static <C extends AbstractWorldConfig> void reload(
        final Class<C> configClass,
        final Map<UUID, C> configMap,
        final Function<World, C> factory
    ) {
        configMap.clear();
        Bukkit.getWorlds().forEach(factory::apply);
        final WorldManager worldManager = SquaremapPlugin.getInstance().worldManager();
        if (worldManager != null) {
            for (final MapWorld world : worldManager.worlds().values()) {
                world.refreshConfigInstances();
            }
        }
    }

}
