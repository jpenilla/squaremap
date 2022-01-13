package xyz.jpenilla.squaremap.paper.util;

import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;

public final class CraftBukkitReflection {
    private CraftBukkitReflection() {
    }

    private static final String PREFIX_CRAFTBUKKIT = "org.bukkit.craftbukkit";
    private static final String CRAFT_SERVER = "CraftServer";
    private static final String CB_PKG_VERSION;

    static {
        final Class<?> serverClass = Bukkit.getServer().getClass();
        String name = serverClass.getName();
        name = name.substring(PREFIX_CRAFTBUKKIT.length());
        name = name.substring(0, name.length() - CRAFT_SERVER.length());
        CB_PKG_VERSION = name;
    }

    public static @NonNull Class<?> needOBCClass(final @NonNull String className) {
        return ReflectionUtil.needClass(PREFIX_CRAFTBUKKIT + CB_PKG_VERSION + className);
    }

    private static final Class<?> CRAFT_WORLD_CLASS = needOBCClass("CraftWorld");
    private static final Method CRAFT_WORLD_GET_HANDLE = ReflectionUtil.needMethod(CRAFT_WORLD_CLASS, List.of("getHandle"));

    public static @NonNull ServerLevel serverLevel(final @NonNull World world) {
        try {
            return (ServerLevel) CRAFT_WORLD_GET_HANDLE.invoke(world);
        } catch (final ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final Class<?> CRAFT_PLAYER_CLASS = needOBCClass("entity.CraftPlayer");
    private static final Method CRAFT_PLAYER_GET_HANDLE = ReflectionUtil.needMethod(CRAFT_PLAYER_CLASS, List.of("getHandle"));

    public static @NonNull ServerPlayer serverPlayer(final @NonNull Player player) {
        try {
            return (ServerPlayer) CRAFT_PLAYER_GET_HANDLE.invoke(player);
        } catch (final ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final Method LEVEL_GET_WORLD = ReflectionUtil.needMethod(Level.class, List.of("getWorld"));

    public static @NonNull World world(final @NonNull ServerLevel level) {
        try {
            return (World) LEVEL_GET_WORLD.invoke(level);
        } catch (final ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
