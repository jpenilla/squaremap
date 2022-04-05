package xyz.jpenilla.squaremap.common.config;

import io.leangen.geantyref.TypeToken;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;
import xyz.jpenilla.squaremap.common.util.Util;

@SuppressWarnings("unused")
public abstract class AbstractWorldConfig<C extends AbstractConfig> {
    private static final @Nullable Class<?> PAPER_MIGRATION_CLASS = ReflectionUtil.findClass("xyz.jpenilla.squaremap.paper.util.WorldNameToKeyMigration");
    private static final @Nullable Method PAPER_MIGRATE_METHOD = PAPER_MIGRATION_CLASS == null ? null : ReflectionUtil.needMethod(PAPER_MIGRATION_CLASS, List.of("migrate"), AbstractConfig.class, ServerLevel.class);
    public static final String DOT = "____dot____";

    final String worldName;
    protected final ServerLevel world;
    protected final AbstractConfig parent;
    private final Class<? extends AbstractWorldConfig<?>> configClass;

    protected AbstractWorldConfig(
        final Class<? extends AbstractWorldConfig<?>> worldConfigClass,
        final AbstractConfig parent,
        final ServerLevel level
    ) {
        this.configClass = worldConfigClass;
        this.world = level;
        this.worldName = Util.levelConfigName(level)
            .replace(".", DOT); // replace '.' as we later split on it (see AbstractConfig.splitPath)
        this.parent = parent;

        // hack but works
        if (PAPER_MIGRATE_METHOD != null) {
            ReflectionUtil.invokeOrThrow(PAPER_MIGRATE_METHOD, null, parent, level);
        }
    }

    /*
    void set(final String path, final Object val) {
        this.parent.yaml.addDefault("world-settings.default." + path, val);
        this.parent.yaml.set("world-settings.default." + path, val);
        if (this.parent.yaml.get("world-settings." + this.worldName + "." + path) != null) {
            this.parent.yaml.addDefault("world-settings." + this.worldName + "." + path, val);
            this.parent.yaml.set("world-settings." + this.worldName + "." + path, val);
        }
    }
     */

    protected final void init() {
        this.parent.readConfig(this.configClass, this);
    }

    protected final boolean getBoolean(String path, boolean def) {
        if (this.virtual(this.wrapPath(path))) {
            return this.parent.getBoolean(wrapDefaultPath(path), def);
        }
        return this.parent.getBoolean(this.wrapPath(path), this.parent.getBoolean(wrapDefaultPath(path), def));
    }

    protected final int getInt(String path, int def) {
        if (this.virtual(this.wrapPath(path))) {
            return this.parent.getInt(wrapDefaultPath(path), def);
        }
        return this.parent.getInt(this.wrapPath(path), this.parent.getInt(wrapDefaultPath(path), def));
    }

    protected final double getDouble(String path, double def) {
        if (this.virtual(this.wrapPath(path))) {
            return this.parent.getDouble(wrapDefaultPath(path), def);
        }
        return this.parent.getDouble(this.wrapPath(path), this.parent.getDouble(wrapDefaultPath(path), def));
    }

    protected final String getString(String path, String def) {
        if (this.virtual(this.wrapPath(path))) {
            return this.parent.getString(wrapDefaultPath(path), def);
        }
        return this.parent.getString(this.wrapPath(path), this.parent.getString(wrapDefaultPath(path), def));
    }

    protected final <T> List<T> getList(TypeToken<T> elementType, String path, List<T> def) {
        if (this.virtual(this.wrapPath(path))) {
            return this.parent.getList(elementType, wrapDefaultPath(path), def);
        }
        return this.parent.getList(elementType, this.wrapPath(path), this.parent.getList(elementType, wrapDefaultPath(path), def));
    }

    protected final <T> List<T> getList(Class<T> elementType, String path, List<T> def) {
        return this.getList(TypeToken.get(elementType), path, def);
    }

    protected final List<String> getStringList(String path, List<String> def) {
        return this.getList(String.class, path, def);
    }

    protected final <T> T get(final TypeToken<T> type, final String path, final T def) {
        if (this.virtual(this.wrapPath(path))) {
            return this.parent.get(type, wrapDefaultPath(path), def);
        }
        return this.parent.get(type, this.wrapPath(path), this.parent.get(type, wrapDefaultPath(path), def));
    }

    private boolean virtual(String path) {
        return this.parent.config.node(Config.splitPath(path)).virtual();
    }

    private String wrapPath(final String path) {
        return "world-settings." + this.worldName + "." + path;
    }

    private static String wrapDefaultPath(final String path) {
        return "world-settings.default." + path;
    }
}
