package net.pl3x.map.util;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Field;

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
}
