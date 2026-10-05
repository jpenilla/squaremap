package xyz.jpenilla.squaremap.common.util;

@FunctionalInterface
public interface CheckedRunnable<X extends Throwable> {
    void run() throws X;

    static <X extends Throwable> Runnable unchecked(final CheckedRunnable<X> runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (final Throwable thr) {
                Exceptions.rethrow(thr);
            }
        };
    }
}
