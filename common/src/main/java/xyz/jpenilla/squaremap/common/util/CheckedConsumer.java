package xyz.jpenilla.squaremap.common.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface CheckedConsumer<T, X extends Throwable> {
    void accept(T t) throws X;

    static <T, X extends Throwable> Consumer<T> unchecked(final CheckedConsumer<T, X> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (final Throwable thr) {
                Exceptions.rethrow(thr);
            }
        };
    }
}
