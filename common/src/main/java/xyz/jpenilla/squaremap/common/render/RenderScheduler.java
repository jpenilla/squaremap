package xyz.jpenilla.squaremap.common.render;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.concurrent.Threads;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;

/**
 * Runs a world's render jobs one at a time on a single thread, and schedules background updates.
 *
 * <p>Starting a render does not wait for render work. Cancelling waits only for the job to stop,
 * not for its queued image saves.</p>
 */
@DefaultQualifier(NonNull.class)
public final class RenderScheduler {
    private final MapWorldInternal world;
    private final RenderFactory factory;
    private final RenderCheckpointStore checkpoints;
    private final ScheduledExecutorService executor;
    private @Nullable Future<?> background;
    private @Nullable Task foreground;
    private @Nullable Task running;
    private boolean paused;
    private boolean closed;

    private RenderScheduler(final MapWorldInternal world, final RenderFactory factory) {
        this.world = world;
        this.factory = factory;
        this.checkpoints = new RenderCheckpointStore(world.dataPath().resolve("resume_render.json"));
        this.executor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("squaremap-render-[" + world.identifier().asString() + "]").factory());
    }

    public void init() {
        final RenderCheckpointStore.@Nullable Checkpoint checkpoint = this.checkpoints.read();
        if (checkpoint != null) {
            this.startForeground(new Task(checkpoint.mode(), Messages.LOG_RESUMED_RENDERING,
                () -> this.factory.resumeRender(this.world, checkpoint, this.checkpoints)));
        } else {
            this.startBackground();
        }
    }

    public synchronized boolean isRendering() {
        return this.foreground != null;
    }

    public synchronized void pauseRenders(final boolean paused) {
        this.paused = paused;
        final @Nullable RenderJob job = this.runningJob();
        if (job != null) {
            job.pause(paused);
        }
    }

    public synchronized boolean rendersPaused() {
        return this.paused;
    }

    public synchronized void restartRenderProgressLogging() {
        final @Nullable RenderJob job = this.runningJob();
        if (job != null) {
            job.restartProgressLogger();
        }
    }

    private @Nullable RenderJob runningJob() {
        return this.running == null ? null : this.running.job;
    }

    /**
     * Starts a full render without waiting for it to begin.
     *
     * @return {@code false} if a full or radius render is already active
     */
    public boolean startFullRender() {
        return this.startForeground(new Task(RenderJob.Mode.FULL, Messages.LOG_STARTED_FULLRENDER,
            () -> this.factory.createFullRender(this.world, this.checkpoints)));
    }

    /**
     * Starts a radius render without waiting for it to begin.
     *
     * @param center the center block
     * @param radius the radius in blocks
     * @return {@code false} if a full or radius render is already active
     */
    public boolean startRadiusRender(final BlockPos center, final int radius) {
        return this.startForeground(new Task(RenderJob.Mode.RADIUS, Messages.LOG_STARTED_RADIUSRENDER,
            () -> this.factory.createRadiusRender(this.world, center, radius, this.checkpoints)));
    }

    private synchronized boolean startForeground(final Task task) {
        if (this.closed) {
            throw new IllegalStateException("Render scheduler is closed");
        }
        if (this.foreground != null) {
            return false;
        }
        this.cancelBackgroundSchedule();
        // With no foreground task, a running task is a background job.
        if (this.running != null) {
            this.running.stop(RenderOutcome.STOPPED);
        }
        this.foreground = task;
        this.executor.execute(() -> this.run(task));
        return true;
    }

    /**
     * Cancels the active full or radius render, waiting for the job to stop. Image saves it
     * already queued may still be in progress when this returns.
     *
     * @return {@code false} if no full or radius render is active
     */
    public boolean cancelRender() {
        final Task task;
        synchronized (this) {
            task = this.foreground;
            if (task == null) {
                return false;
            }
            task.stop(RenderOutcome.CANCELLED);
        }
        Threads.awaitUninterruptibly(task.done);
        return true;
    }

    private synchronized void startBackground() {
        if (this.closed || this.background != null || this.foreground != null || !this.world.config().BACKGROUND_RENDER.enabled) {
            return;
        }
        final long interval = this.world.config().BACKGROUND_RENDER.intervalSeconds;
        this.background = this.executor.scheduleAtFixedRate(() -> {
            if (this.world.hasModifiedChunks()) {
                this.run(new Task(RenderJob.Mode.BACKGROUND, null, () -> this.factory.createBackgroundRender(this.world)));
            }
        }, interval, interval, TimeUnit.SECONDS);
    }

    private void cancelBackgroundSchedule() {
        if (this.background != null) {
            this.background.cancel(false);
            this.background = null;
        }
    }

    private void run(final Task task) {
        RenderOutcome outcome = RenderOutcome.FAILED;
        try {
            synchronized (this) {
                // A background tick that fired before a foreground start or shutdown must not run.
                if (task.requested != null || !task.mode.foreground() && (this.closed || this.foreground != null)) {
                    return;
                }
                this.running = task;
                task.runner = Thread.currentThread();
            }
            if (task.startMessage != null) {
                Logging.info(task.startMessage, "world", this.world.identifier().asString());
            }
            final RenderJob job = task.create.call();
            synchronized (this) {
                task.job = job;
                job.pause(this.paused);
                if (task.requested != null) {
                    job.stop(task.requested);
                }
            }
            outcome = job.execute();
        } catch (final InterruptedException ignore) {
            outcome = RenderOutcome.STOPPED;
        } catch (final Exception ex) {
            if (task.requested == null) {
                Logging.logger().error("Exception preparing {} render for {}", task.mode, this.world.identifier().asString(), ex);
            }
        } finally {
            synchronized (this) {
                if (this.running == task) {
                    this.running = null;
                }
                task.runner = null;
                if (task.requested != null) {
                    outcome = task.requested;
                }
            }
            // Clear any interrupt from a stop request so it cannot reach the next task.
            Thread.interrupted();
            this.finish(task, outcome);
        }
    }

    private void finish(final Task task, final RenderOutcome outcome) {
        try {
            if (task.mode.foreground()) {
                if (outcome != RenderOutcome.STOPPED) {
                    this.checkpoints.delete();
                }
                this.logCompletion(outcome);
            } else {
                Logging.debug(() -> String.format("Finished background render cycle for %s in %.2f seconds",
                    this.world.identifier().asString(), (System.nanoTime() - task.created) / 1_000_000_000.0D));
            }
        } finally {
            if (task.mode.foreground()) {
                synchronized (this) {
                    if (this.foreground == task) {
                        this.foreground = null;
                    }
                }
                this.startBackground();
            }
            task.done.countDown();
        }
    }

    private void logCompletion(final RenderOutcome outcome) {
        final String world = this.world.identifier().asString();
        if (outcome == RenderOutcome.FAILED) {
            Logging.logger().error("Rendering failed for world '{}'", world);
        } else {
            Logging.info(outcome == RenderOutcome.COMPLETED ? Messages.LOG_FINISHED_RENDERING : Messages.LOG_CANCELLED_RENDERING, "world", world);
        }
    }

    /**
     * Stops rendering and waits for the render thread to finish. Saved progress of a stopped
     * full or radius render is kept. Queued image saves are not awaited.
     */
    public void shutdown() {
        synchronized (this) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            this.cancelBackgroundSchedule();
            if (this.running != null) {
                this.running.stop(RenderOutcome.STOPPED);
            }
            if (this.foreground != null) {
                this.foreground.stop(RenderOutcome.STOPPED);
            }
        }
        this.executor.shutdown();
        Threads.awaitTerminationUninterruptibly(this.executor);
    }

    private static final class Task {
        private final RenderJob.Mode mode;
        private final @Nullable String startMessage;
        private final Callable<RenderJob> create;
        private final CountDownLatch done = new CountDownLatch(1);
        private final long created = System.nanoTime();
        // Written while holding the scheduler monitor.
        private @Nullable Thread runner;
        private @Nullable RenderJob job;
        private volatile @Nullable RenderOutcome requested;

        private Task(final RenderJob.Mode mode, final @Nullable String startMessage, final Callable<RenderJob> create) {
            this.mode = mode;
            this.startMessage = startMessage;
            this.create = create;
        }

        /**
         * Requests the task stop. Must be called while holding the scheduler monitor.
         */
        private void stop(final RenderOutcome outcome) {
            if (this.requested != null) {
                return;
            }
            this.requested = outcome;
            if (this.job != null) {
                this.job.stop(outcome);
            }
            // Interrupt planning, and job waits that cannot observe the stop request directly.
            if (this.runner != null) {
                this.runner.interrupt();
            }
        }

    }

    public static RenderScheduler create(final MapWorldInternal world, final RenderFactory factory) {
        return new RenderScheduler(world, factory);
    }
}
