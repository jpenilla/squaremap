package xyz.jpenilla.squaremap.common.util;

@FunctionalInterface
public interface CheckedConsumer<T, X extends Throwable> {
    void accept(T t) throws X;
}
