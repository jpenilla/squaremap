package net.pl3x.map.plugin.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ReflectionUtil {
    private ReflectionUtil() {
    }

    public static @NonNull Field needField(final @NonNull Class<?> clazz, final @NonNull String name) {
        try {
            final Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (final NoSuchFieldException ex) {
            throw new IllegalStateException(
                    String.format(
                            "Could not locate field '%s' in class '%s'",
                            name,
                            clazz.getSimpleName()
                    ),
                    ex
            );
        }
    }

    public static @NonNull Method needMethod(final @NonNull Class<?> clazz, final @NonNull String name) {
        try {
            final Method method = clazz.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (final NoSuchMethodException ex) {
            throw new IllegalStateException(
                    String.format(
                            "Could not locate method '%s' in class '%s'",
                            name,
                            clazz.getSimpleName()
                    ),
                    ex
            );
        }
    }

    public static @Nullable Object invokeOrThrow(final @NonNull Method method, final @Nullable Object instance, final Object @NonNull ... args) {
        try {
            return method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke method, e");
        }
    }
}
