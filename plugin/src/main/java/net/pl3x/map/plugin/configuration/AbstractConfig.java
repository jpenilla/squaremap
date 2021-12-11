package net.pl3x.map.plugin.configuration;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.pl3x.map.plugin.Logging;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"unused", "SameParameterValue"})
abstract class AbstractConfig {
    final File file;
    final YamlConfiguration yaml;

    AbstractConfig(String filename) {
        this.file = new File(Pl3xMapPlugin.getInstance().getDataFolder(), filename);
        this.yaml = new YamlConfiguration();
        try {
            this.yaml.load(file);
        } catch (IOException ignore) {
        } catch (InvalidConfigurationException ex) {
            Logging.severe(String.format("Could not load %s, please correct your syntax errors", filename));
            throw new RuntimeException(ex);
        }
        this.yaml.options().copyDefaults(true);
    }

    void readConfig(Class<?> clazz, Object instance) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPrivate(method.getModifiers())) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE) {
                    try {
                        method.setAccessible(true);
                        method.invoke(instance);
                    } catch (InvocationTargetException ex) {
                        throw new RuntimeException(ex.getCause());
                    } catch (Exception ex) {
                        Logging.severe("Error invoking " + method);
                        ex.printStackTrace();
                    }
                }
            }
        }

        this.save();
    }

    public void save() {
        try {
            this.yaml.save(file);
        } catch (final IOException ex) {
            Logging.severe("Could not save " + this.file, ex);
        }
    }

    void set(String path, Object val) {
        this.yaml.addDefault(path, val);
        this.yaml.set(path, val);
    }

    String getString(String path, String def) {
        this.yaml.addDefault(path, def);
        return this.yaml.getString(path, this.yaml.getString(path));
    }

    boolean getBoolean(String path, boolean def) {
        this.yaml.addDefault(path, def);
        return this.yaml.getBoolean(path, this.yaml.getBoolean(path));
    }

    int getInt(String path, int def) {
        this.yaml.addDefault(path, def);
        return this.yaml.getInt(path, this.yaml.getInt(path));
    }

    double getDouble(String path, double def) {
        this.yaml.addDefault(path, def);
        return this.yaml.getDouble(path, this.yaml.getDouble(path));
    }

    <T> List<?> getList(String path, T def) {
        this.yaml.addDefault(path, def);
        return this.yaml.getList(path, this.yaml.getList(path));
    }

    @NonNull <T> Map<String, T> getMap(final @NonNull String path, final @Nullable Map<String, T> def) {
        final ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
        if (def != null && this.yaml.getConfigurationSection(path) == null) {
            //def.forEach((key, value) -> yaml.addDefault(path + "." + key, value));
            this.yaml.addDefault(path, def.isEmpty() ? new HashMap<>() : def);
            return def;
        }
        final ConfigurationSection section = this.yaml.getConfigurationSection(path);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                @SuppressWarnings("unchecked") final T val = (T) section.get(key);
                if (val != null) {
                    builder.put(key, val);
                }
            }
        }
        return builder.build();
    }

}
