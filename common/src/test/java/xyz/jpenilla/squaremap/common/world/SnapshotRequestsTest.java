package xyz.jpenilla.squaremap.common.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotRequestsTest {
    @Test
    void limitChangesAccountForOutstandingOperations() throws Exception {
        final SnapshotRequests requests = new SnapshotRequests(2);
        requests.acquire(2);
        requests.limit(1);
        requests.release();
        assertFalse(requests.tryAcquire());
        requests.limit(2);
        assertTrue(requests.tryAcquire());
        assertFalse(requests.tryAcquire());
        requests.release(2);
        assertEquals(2, requests.availablePermits());
    }
}
