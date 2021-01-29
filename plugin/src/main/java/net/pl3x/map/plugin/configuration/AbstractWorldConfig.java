package net.pl3x.map.plugin.configuration;

import org.bukkit.World;

import java.util.List;

@SuppressWarnings("unused")
abstract class AbstractWorldConfig {
    final String worldName;
    final AbstractConfig config;

    AbstractWorldConfig(World world, AbstractConfig parent) {
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

}
