package xyz.jpenilla.squaremap.plugin.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ReflectionUtil {
    private ReflectionUtil() {
    }

    @SafeVarargs
    public static @NonNull <T> T firstNonNullOrThrow(final @NonNull Supplier<String> exceptionMessage, final @Nullable T @NonNull ... elements) {
        for (final T element : elements) {
            if (element != null) {
                return element;
            }
        }
        throw new IllegalArgumentException(exceptionMessage.get());
    }

    public static @Nullable Field findField(final @NonNull Class<?> clazz, final @NonNull String name) {
        try {
            final Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (final NoSuchFieldException ex) {
            return null;
        }
    }

    public static @Nullable Method findMethod(final @NonNull Class<?> clazz, final @NonNull String name, final Class<?> @NonNull ... parameterTypes) {
        try {
            final Method method = clazz.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (final NoSuchMethodException ex) {
            return null;
        }
    }

    public static @NonNull Class<?> needClass(final @NonNull String className) {
        try {
            return Class.forName(className);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NonNull Field needField(final @NonNull Class<?> clazz, final @NonNull String @NonNull ... names) {
        return firstNonNullOrThrow(
                () -> String.format("Could not locate field in class '%s' with any of the following names: %s", clazz.getName(), Arrays.toString(names)),
                Arrays.stream(names)
                        .map(name -> findField(clazz, name))
                        .toArray(Field[]::new)
        );
    }

    public static @NonNull Method needMethod(final @NonNull Class<?> clazz, final @NonNull List<@NonNull String> names, final Class<?> @NonNull ... parameterTypes) {
        return firstNonNullOrThrow(
                () -> String.format("Could not locate method in class '%s' with any of the following names: [%s]", clazz.getName(), String.join(", ")),
                names.stream()
                        .map(name -> findMethod(clazz, name, parameterTypes))
                        .toArray(Method[]::new)
        );
    }

    public static @Nullable Object invokeOrThrow(final @NonNull Method method, final @Nullable Object instance, final Object @NonNull ... args) {
        try {
            return method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke method", e);
        }
    }

    public static final class CraftBukkit {
        private CraftBukkit() {
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
            return needClass(PREFIX_CRAFTBUKKIT + CB_PKG_VERSION + className);
        }

        private static final Class<?> CRAFT_WORLD_CLASS = needOBCClass("CraftWorld");
        private static final Method CRAFT_WORLD_GET_HANDLE = needMethod(CRAFT_WORLD_CLASS, List.of("getHandle"));

        public static @NonNull ServerLevel serverLevel(final @NonNull World world) {
            try {
                return (ServerLevel) CRAFT_WORLD_GET_HANDLE.invoke(world);
            } catch (final ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
