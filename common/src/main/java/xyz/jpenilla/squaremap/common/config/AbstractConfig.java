package xyz.jpenilla.squaremap.common.config;

import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.SquaremapCommon;

import static java.util.Objects.requireNonNull;
import static xyz.jpenilla.squaremap.common.util.Util.rethrow;

@SuppressWarnings({"unused", "SameParameterValue"})
public abstract class AbstractConfig {
    final Path configFile;
    final ConfigurationNode config;
    private final ConfigurationLoader<CommentedConfigurationNode> loader;
    private final Class<? extends AbstractConfig> configClass;

    protected AbstractConfig(final Class<? extends AbstractConfig> configClass, final String filename) {
        this.configClass = configClass;
        this.configFile = SquaremapCommon.instance().platform().dataDirectory().resolve(filename);

        this.loader = YamlConfigurationLoader.builder()
            .path(this.configFile)
            .nodeStyle(NodeStyle.BLOCK)
            .build();

        try {
            this.config = this.loader.load();
        } catch (final ConfigurateException ex) {
            throw new RuntimeException("Could not load config.yml, exception occurred (are there syntax errors?)", ex);
        }

        this.upgradeConfig();
    }

    protected void addVersions(final ConfigurationTransformation.VersionedBuilder versionedBuilder) {
    }

    private ConfigUpgrader createUpgrader() {
        return new ConfigUpgrader(builder -> {
            builder.versionKey("config-version");
            builder.addVersion(1, ConfigurationTransformation.empty());
            this.addVersions(builder);
        });
    }

    private void upgradeConfig() {
        final ConfigUpgrader.UpgradeResult<@NonNull ConfigurationNode> result = this.createUpgrader().upgrade(this.config);
        if (result.didUpgrade()) {
            Logging.debug(() -> "Upgraded %s from %s to %s".formatted(this.configClass.getName(), result.originalVersion(), result.newVersion()));
        }
    }

    final void readConfig(final Class<?> clazz, final Object instance) {
        for (final Method method : clazz.getDeclaredMethods()) {
            if (!Modifier.isPrivate(method.getModifiers()) || method.getParameterTypes().length != 0 || method.getReturnType() != Void.TYPE) {
                continue;
            }

            try {
                method.setAccessible(true);
                method.invoke(instance);
            } catch (final InvocationTargetException ex) {
                Logging.logger().error("Error invoking {}", method, ex.getCause());
            } catch (final Exception ex) {
                Logging.logger().error("Error invoking {}", method, ex);
            }
        }

        this.save();
    }

    public void save() {
        try {
            this.loader.save(this.config);
        } catch (final IOException ex) {
            Logging.logger().error("Could not save {}", this.configFile, ex);
        }
    }

    private ConfigurationNode node(String path) {
        return this.config.node(splitPath(path));
    }

    protected final void set(String path, Object val) {
        try {
            this.node(path).set(val);
        } catch (SerializationException e) {
            rethrow(e);
        }
    }

    protected final String getString(String path, String def) {
        return this.node(path).getString(def);
    }

    public final boolean getBoolean(String path, boolean def) {
        return this.node(path).getBoolean(def);
    }

    protected final int getInt(String path, int def) {
        final ConfigurationNode node = this.node(path);
        // manually set default (see getInt(int) impl)
        if (node.virtual()) {
            try {
                node.set(def);
            } catch (final SerializationException e) {
                rethrow(e);
            }
        }
        return node.getInt(def);
    }

    protected final double getDouble(String path, double def) {
        final ConfigurationNode node = this.node(path);
        // manually set default (see getDouble(double) impl)
        if (node.virtual()) {
            try {
                node.set(def);
            } catch (final SerializationException e) {
                rethrow(e);
            }
        }
        return node.getDouble(def);
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
        final String[] split = path.split("\\.");
        // replace AbstractWorldConfig.DOT back to '.' after split
        for (int i = 0; i < split.length; i++) {
            final String s = split[i];
            if (s.contains(AbstractWorldConfig.DOT)) {
                split[i] = s.replace(AbstractWorldConfig.DOT, ".");
            }
        }
        return split;
    }
}
