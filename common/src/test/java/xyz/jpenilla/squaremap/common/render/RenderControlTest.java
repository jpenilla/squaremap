package xyz.jpenilla.squaremap.common.render;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(10)
class RenderControlTest {
    @Test
    void cancellationPreservesTheFirstExplicitStopReason() {
        final RenderControl control = new RenderControl();
        control.cancel();
        assertNull(control.requested());
        assertThrows(InterruptedException.class, control::await);
        control.stop(RenderOutcome.STOPPED);
        control.stop(RenderOutcome.CANCELLED);
        control.pause(false);
        assertEquals(RenderOutcome.STOPPED, control.requested());
        assertTrue(control.cancelled());
        assertThrows(InterruptedException.class, control::await);
    }

    @Test
    void cancellationWakesPausedWaiters() throws Exception {
        final RenderControl gate = new RenderControl();
        gate.pause(true);
        final FutureTask<Void> task = new FutureTask<>(() -> {
            gate.await();
            return null;
        });
        final Thread waiter = Thread.ofVirtual().start(task);
        try {
            while (waiter.getState() != Thread.State.WAITING) {
                assertFalse(task.isDone());
                Thread.sleep(1);
            }
            gate.cancel();
            final ExecutionException failure = assertThrows(ExecutionException.class, () -> task.get(1, TimeUnit.SECONDS));
            assertInstanceOf(InterruptedException.class, failure.getCause());
        } finally {
            gate.cancel();
            waiter.join(1000);
        }
    }
}
