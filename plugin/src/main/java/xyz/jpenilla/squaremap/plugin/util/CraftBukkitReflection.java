package xyz.jpenilla.squaremap.plugin.util;

import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.World;
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
}
