package xyz.jpenilla.squaremap.common.world;

import java.util.concurrent.Semaphore;

/**
 * Limits outstanding snapshot loads for a world, shared by successive render jobs.
 * Stopping a job does not release permits for loads still running on the platform;
 * those permits are released only when the loads finish.
 */
public final class SnapshotRequests extends Semaphore {
    private int limit;

    public SnapshotRequests(final int limit) {
        super(limit);
        this.limit = limit;
    }

    public synchronized void limit(final int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Snapshot request limit must be positive");
        }
        final int difference = limit - this.limit;
        if (difference > 0) {
            this.release(difference);
        } else if (difference < 0) {
            this.reducePermits(-difference);
        }
        this.limit = limit;
    }
}
