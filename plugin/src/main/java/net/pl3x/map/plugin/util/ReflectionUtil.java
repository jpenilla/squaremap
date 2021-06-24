package net.pl3x.map.plugin.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
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
}
