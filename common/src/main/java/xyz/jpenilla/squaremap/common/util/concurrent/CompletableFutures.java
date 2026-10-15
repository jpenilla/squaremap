package xyz.jpenilla.squaremap.common.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

public final class CompletableFutures {
    private CompletableFutures() {
    }

    /**
     * Runs the supplier on the executor and completes with the result of the stage it returns, like
     * {@link CompletableFuture#thenComposeAsync(Function, Executor)} without a preceding stage.
     *
     * @param supplier supplies the stage to compose; run on {@code executor}
     * @param executor executor to run {@code supplier} on
     * @param <T>      result type
     * @return a future completed with the result of the supplied stage, or exceptionally if
     *     {@code supplier} throws or the supplied stage completes exceptionally
     */
    public static <T> CompletableFuture<T> composeAsync(final Supplier<? extends CompletionStage<T>> supplier, final Executor executor) {
        return CompletableFuture.supplyAsync(supplier, executor).thenCompose(Function.identity());
    }
}
