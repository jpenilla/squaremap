package net.pl3x.map.plugin.configuration;

import net.minecraft.server.level.ServerLevel;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
abstract class AbstractWorldConfig {
    final ServerLevel world;
    final String worldName;
    final AbstractConfig config;

    AbstractWorldConfig(World world, AbstractConfig parent) {
        this.world = ((CraftWorld) world).getHandle();
        this.worldName = world.getName();
        this.config = parent;
    }

    void set(String path, Object val) {
        config.yaml.addDefault("world-settings.default." + path, val);
        config.yaml.set("world-settings.default." + path, val);
        if (config.yaml.get("world-settings." + worldName + "." + path) != null) {
            config.yaml.addDefault("world-settings." + worldName + "." + path, val);
            config.yaml.set("world-settings." + worldName + "." + path, val);
        }
    }

    boolean getBoolean(String path, boolean def) {
        config.yaml.addDefault("world-settings.default." + path, def);
        return config.yaml.getBoolean("world-settings." + worldName + "." + path,
                config.yaml.getBoolean("world-settings.default." + path));
    }

    int getInt(String path, int def) {
        config.yaml.addDefault("world-settings.default." + path, def);
        return config.yaml.getInt("world-settings." + worldName + "." + path,
                config.yaml.getInt("world-settings.default." + path));
    }

    String getString(String path, String def) {
        config.yaml.addDefault("world-settings.default." + path, def);
        return config.yaml.getString("world-settings." + worldName + "." + path,
                config.yaml.getString("world-settings.default." + path));
    }

    <T> List<?> getList(String path, T def) {
        config.yaml.addDefault("world-settings.default." + path, def);
        return config.yaml.getList("world-settings." + worldName + "." + path,
                config.yaml.getList("world-settings.default." + path));
    }

    @NonNull <T> Map<String, T> getMap(final @NonNull String path, final @Nullable Map<String, T> def) {
        final Map<String, T> fallback = config.getMap("world-settings.default." + path, def);
        final Map<String, T> value = config.getMap("world-settings." + worldName + "." + path, null);
        return value.isEmpty() ? fallback : value;
    }

}
