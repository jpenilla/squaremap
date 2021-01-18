package net.pl3x.map.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * Data class for holding a pair of non-null objects with generic types.
 *
 * @param <L> Left generic type argument
 * @param <R> Right generic type argument
 */
public final class Pair<L, R> {
    private final L left;
    private final R right;

    private Pair(final @NonNull L left, final @NonNull R right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Get the left value
     *
     * @return Left value
     */
    public @NonNull L left() {
        return this.left;
    }

    /**
     * Get the right value
     *
     * @return Right value
     */
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

    /**
     * Create a new Pair
     *
     * @param left  Left value
     * @param right Right value
     * @param <L>   Left generic type
     * @param <R>   Right generic type
     * @return new pair
     */
    public static <L, R> @NonNull Pair<L, R> of(final @NonNull L left, final @NonNull R right) {
        return new Pair<>(left, right);
    }
}
