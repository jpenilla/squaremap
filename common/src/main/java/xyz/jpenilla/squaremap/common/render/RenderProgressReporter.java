package xyz.jpenilla.squaremap.common.render;

import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;

@DefaultQualifier(NonNull.class)
final class RenderProgressReporter implements AutoCloseable {
    private static final int ROLLING_AVG_SIZE = 20;
    private final DecimalFormat percentFormat = new DecimalFormat("0.00%");
    private final DecimalFormat rateFormat = new DecimalFormat("0.0");

    private final RenderJob render;
    private final ScheduledExecutorService timer;
    private final long startTime = System.nanoTime();
    private final int[] rollingAvgCps = new int[ROLLING_AVG_SIZE];

    private int rollingAvgIndex;
    private int rollingSamples;
    private long rollingChunks;
    private long sampledChunks;
    private long samples;
    private int prevChunks;
    private int untilLog;
    private boolean closed;

    RenderProgressReporter(final RenderJob render) {
        this.render = render;
        this.prevChunks = this.render.processedChunks();
        this.timer = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual()
            .name("squaremap-render-progresslogger-[" + render.world().identifier().asString() + "]").factory());
        this.timer.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    synchronized void restartLogging() {
        this.untilLog = 0;
    }

    private synchronized void tick() {
        if (this.closed || this.render.paused()) {
            return;
        }

        final int curChunks = this.render.processedChunks();
        final int diff = curChunks - this.prevChunks;
        this.prevChunks = curChunks;

        this.rollingChunks += diff - this.rollingAvgCps[this.rollingAvgIndex];
        this.rollingAvgCps[this.rollingAvgIndex] = diff;
        this.rollingAvgIndex = (this.rollingAvgIndex + 1) % ROLLING_AVG_SIZE;
        this.rollingSamples = Math.min(this.rollingSamples + 1, ROLLING_AVG_SIZE);
        this.sampledChunks += diff;
        this.samples++;

        // Continue sampling while logging is disabled, so toggling it does not lose rate history.
        if (!Config.PROGRESS_LOGGING || this.untilLog-- > 0) {
            return;
        }
        this.untilLog = Math.max(1, Config.PROGRESS_LOGGING_INTERVAL) - 1;
        this.log(curChunks);
    }

    private void log(final int curChunks) {
        final int totalChunks = this.render.totalChunks();
        final int chunksLeft = Math.max(0, totalChunks - curChunks);
        final double average = (double) this.sampledChunks / this.samples;
        final String eta = chunksLeft == 0 ? formatMilliseconds(0)
            : average > 0 ? formatMilliseconds((long) (chunksLeft * 1000.0D / average)) : "unknown";
        final double percent = totalChunks == 0 ? 1.0D : (double) curChunks / totalChunks;
        final int totalRegions = this.render.totalRegions();

        Logging.info(
            (totalRegions > 0 ? Messages.LOG_RENDER_PROGRESS_WITH_REGIONS : Messages.LOG_RENDER_PROGRESS),
            "world", this.render.world().identifier().asString(),
            "current_regions", this.render.processedRegions(),
            "total_regions", totalRegions,
            "current_chunks", curChunks,
            "total_chunks", totalChunks,
            "percent", this.percentFormat.format(percent),
            "elapsed", formatMilliseconds(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - this.startTime)),
            "eta", eta,
            "rate", this.rateFormat.format(this.rollingSamples == 0 ? 0.0D : (double) this.rollingChunks / this.rollingSamples)
        );
    }

    /**
     * Stops progress logging. Logs final progress first if progress logging is enabled, since
     * periodic logging rarely lands exactly on completion.
     */
    @Override
    public synchronized void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.timer.shutdownNow();
        if (Config.PROGRESS_LOGGING) {
            this.log(this.render.processedChunks());
        }
    }

    private static String formatMilliseconds(final long timeLeft) {
        final long hrs = TimeUnit.MILLISECONDS.toHours(timeLeft);
        final int min = (int) TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60;
        final int sec = (int) TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60;
        return String.format("%02d:%02d:%02d", hrs, min, sec);
    }
}
