package xyz.jpenilla.squaremap.plugin.config;

import io.leangen.geantyref.TypeToken;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.World;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.WorldManager;
import xyz.jpenilla.squaremap.plugin.data.MapWorld;
import xyz.jpenilla.squaremap.plugin.util.ReflectionUtil;

@SuppressWarnings("unused")
abstract class AbstractWorldConfig {
    final ServerLevel world;
    final String worldName;
    final AbstractConfig parent;

    AbstractWorldConfig(final World world, final AbstractConfig parent) {
        this.world = ReflectionUtil.CraftBukkit.serverLevel(world);
        this.worldName = world.getName();
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
