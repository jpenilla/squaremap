package xyz.jpenilla.squaremap.common.config;

import io.leangen.geantyref.TypeToken;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.WorldManager;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;

@SuppressWarnings("unused")
public abstract class AbstractWorldConfig {
    final String worldName;
    protected final ServerLevel world;
    protected final AbstractConfig parent;

    protected AbstractWorldConfig(final ServerLevel world, final AbstractConfig parent) {
        this.world = world;
        this.worldName = SquaremapCommon.instance().platform().configNameForWorld(world);
        this.parent = parent;
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

    protected static <C extends AbstractWorldConfig> void reload(
        final Class<C> configClass,
        final Map<WorldIdentifier, C> configMap,
        final Function<ServerLevel, C> factory
    ) {
        configMap.clear();
        SquaremapCommon.instance().platform().levels().forEach(factory::apply);
        final WorldManager worldManager = SquaremapCommon.instance().platform().worldManager();
        if (worldManager == null) {
            return;
        }
        for (final MapWorldInternal world : worldManager.worlds().values()) {
            world.refreshConfigInstances();
        }
    }
}
