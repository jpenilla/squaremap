package net.pl3x.map.plugin.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class Pair<L, R> {
    private final L left;
    private final R right;

    private Pair(final @NonNull L left, final @NonNull R right) {
        this.left = left;
        this.right = right;
    }

    public @NonNull L left() {
        return this.left;
    }

    public @NonNull R right() {
        return this.right;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return this.left.equals(pair.left) && this.right.equals(pair.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.left, this.right);
    }

    public static <L, R> @NonNull Pair<L, R> of(final @NonNull L left, final @NonNull R right) {
        return new Pair<>(left, right);
    }
}
