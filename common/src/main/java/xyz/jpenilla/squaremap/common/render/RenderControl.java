package xyz.jpenilla.squaremap.common.render;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Shared pause and cancellation state for one render.
 */
public final class RenderControl {
    private volatile boolean cancelled;
    private boolean paused;
    private volatile @Nullable RenderOutcome requested;

    @Nullable RenderOutcome requested() {
        return this.requested;
    }

    synchronized void pause(final boolean paused) {
        this.paused = paused;
        this.notifyAll();
    }

    synchronized boolean paused() {
        return this.paused;
    }

    synchronized void stop(final RenderOutcome outcome) {
        if (this.requested == null) {
            this.requested = outcome;
            this.cancel();
        }
    }

    synchronized void cancel() {
        this.cancelled = true;
        this.notifyAll();
    }

    public boolean cancelled() {
        return this.cancelled;
    }

    synchronized void await() throws InterruptedException {
        this.checkCancelled();
        while (this.paused) {
            this.wait();
            this.checkCancelled();
        }
    }

    synchronized boolean canStartWork() throws InterruptedException {
        this.checkCancelled();
        return !this.paused;
    }

    private void checkCancelled() throws InterruptedException {
        if (Thread.interrupted() || this.cancelled) {
            throw new InterruptedException("Render cancelled");
        }
    }
}
