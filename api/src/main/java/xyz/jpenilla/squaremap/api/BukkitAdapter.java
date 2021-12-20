package xyz.jpenilla.squaremap.api;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * Helper methods for bridging the Bukkit API with squaremap API.
 */
@DefaultQualifier(NonNull.class)
public final class BukkitAdapter {
    private BukkitAdapter() {
    }

    /**
     * Gets the {@link WorldIdentifier} for a Bukkit {@link World}.
     *
     * @param world Bukkit world
     * @return world identifier
     */
    public static WorldIdentifier worldIdentifier(final World world) {
        return WorldIdentifier.create(world.key().namespace(), world.key().value());
    }

    /**
     * Convert the given {@link WorldIdentifier} to a Bukkit {@link NamespacedKey}.
     *
     * @param worldIdentifier world identifier
     * @return namespaced key
     */
    public static NamespacedKey namespacedKey(final WorldIdentifier worldIdentifier) {
        return Objects.requireNonNull(NamespacedKey.fromString(worldIdentifier.asString()));
    }

    /**
     * Gets the Bukkit {@link World} for the given {@link MapWorld}.
     *
     * @param world world identifier
     * @return bukkit world
     */
    public static World bukkitWorld(final MapWorld world) {
        return Objects.requireNonNull(Bukkit.getWorld(namespacedKey(world.identifier())));
    }

    /**
     * Create a new point from a Bukkit {@link Location}. Uses block location.
     *
     * @param location location
     * @return point
     */
    public static Point point(final Location location) {
        return Point.point(location.getBlockX(), location.getBlockZ());
    }
}
