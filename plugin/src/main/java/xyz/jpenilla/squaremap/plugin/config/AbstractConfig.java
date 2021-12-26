package xyz.jpenilla.squaremap.plugin.config;

import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.jpenilla.squaremap.plugin.Logging;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;

import static java.util.Objects.requireNonNull;
import static xyz.jpenilla.squaremap.plugin.util.Util.rethrow;

@SuppressWarnings({"unused", "SameParameterValue"})
abstract class AbstractConfig {
    final Path configFile;
    final ConfigurationNode config;
    private final ConfigurationLoader<CommentedConfigurationNode> loader;

    protected AbstractConfig(String filename) {
        this.configFile = SquaremapPlugin.getInstance().getDataFolder().toPath().resolve(filename);

        this.loader = YamlConfigurationLoader.builder()
            .path(this.configFile)
            .nodeStyle(NodeStyle.BLOCK)
            .build();

        try {
            this.config = this.loader.load();
        } catch (final ConfigurateException ex) {
            throw new RuntimeException("Could not load config.yml, exception occurred (are there syntax errors?)", ex);
        }
    }

    void readConfig(Class<?> clazz, Object instance) {
        for (final Method method : clazz.getDeclaredMethods()) {
            if (!Modifier.isPrivate(method.getModifiers()) || method.getParameterTypes().length != 0 || method.getReturnType() != Void.TYPE) {
                continue;
            }

            try {
                method.setAccessible(true);
                method.invoke(instance);
            } catch (InvocationTargetException ex) {
                Logging.severe("Error invoking " + method, ex.getCause());
            } catch (final Exception ex) {
                Logging.severe("Error invoking " + method, ex);
            }
        }

        this.save();
    }

    public void save() {
        try {
            this.loader.save(this.config);
        } catch (final IOException ex) {
            Logging.severe("Could not save " + this.configFile, ex);
        }
    }

    private ConfigurationNode node(String path) {
        return this.config.node(splitPath(path));
    }

    void set(String path, Object val) {
        try {
            this.node(path).set(val);
        } catch (SerializationException e) {
            rethrow(e);
        }
    }

    protected final String getString(String path, String def) {
        return this.node(path).getString(def);
    }

    protected final boolean getBoolean(String path, boolean def) {
        return this.node(path).getBoolean(def);
    }

    protected final int getInt(String path, int def) {
        return this.node(path).getInt(def);
    }

    protected final double getDouble(String path, double def) {
        return this.node(path).getDouble(def);
    }

    protected final <T> T get(TypeToken<T> type, final String path, T def) {
        final ConfigurationNode node = this.node(path);
        try {
            final @Nullable T ret = node.virtual() ? null : node.get(type);
            return ret == null ? storeDefault(node, type.getType(), def) : ret;
        } catch (SerializationException e) {
            throw rethrow(e);
        }
    }

    protected final <T> List<T> getList(TypeToken<T> elementType, String path, List<T> def) {
        try {
            //return this.config.node((Object[]) splitPath(path)).getList(elementType, def);
            return this.getList0(elementType, path, def);
        } catch (SerializationException e) {
            throw rethrow(e);
        }
    }

    protected final <T> List<T> getList(Class<T> elementType, String path, List<T> def) {
        try {
            //return this.config.node((Object[]) splitPath(path)).getList(elementType, def);
            return this.getList0(TypeToken.get(elementType), path, def);
        } catch (SerializationException e) {
            throw rethrow(e);
        }
    }

    protected final List<String> getStringList(String path, List<String> def) {
        return this.getList(String.class, path, def);
    }

    @SuppressWarnings("unchecked")
    private <V> List<V> getList0(TypeToken<V> elementType, final String path, List<V> def) throws SerializationException {
        final ConfigurationNode node = this.node(path);
        final Type type = TypeFactory.parameterizedClass(List.class, elementType.getType());
        final @Nullable List<V> ret = node.virtual() ? null : (List<V>) node.get(type);
        return ret == null ? storeDefault(node, type, def) : ret;
    }

    private static <V> V storeDefault(final ConfigurationNode node, final Type type, final V defValue) throws SerializationException {
        requireNonNull(defValue, "defValue");
        if (node.options().shouldCopyDefaults()) {
            node.set(type, defValue);
        }
        return defValue;
    }

    static Object[] splitPath(final String path) {
        return path.split("\\.");
    }
}
