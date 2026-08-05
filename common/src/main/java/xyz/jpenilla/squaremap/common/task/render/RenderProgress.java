package xyz.jpenilla.squaremap.common.task.render;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.Pair;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;

@DefaultQualifier(NonNull.class)
public final class RenderProgress extends TimerTask {
    private static final int ROLLING_AVG_SIZE = 20;
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.00%");
    private static final DecimalFormat RATE_FORMAT = new DecimalFormat("0.0");

    private final AbstractRender render;
    private final long startTime;
    private final int[] rollingAvgCps = new int[ROLLING_AVG_SIZE];
    private final IntList totalAvgCps = new IntArrayList();

    private int rollingAvgIndex = 0;
    private int prevChunks;
    private long seconds;

    private RenderProgress(final AbstractRender render, final @Nullable RenderProgress old) {
        this.render = render;
        this.prevChunks = this.render.processedChunks();
        if (old != null) {
            this.startTime = old.startTime;
            this.rollingAvgIndex = old.rollingAvgIndex;
            this.seconds = old.seconds;
            this.totalAvgCps.addAll(old.totalAvgCps);
            System.arraycopy(old.rollingAvgCps, 0, this.rollingAvgCps, 0, this.rollingAvgCps.length);
        } else {
            this.startTime = System.currentTimeMillis();
        }
    }

    public static @Nullable Pair<Timer, RenderProgress> printProgress(final AbstractRender render) {
        return printProgress(render, null);
    }

    public static @Nullable Pair<Timer, RenderProgress> printProgress(final AbstractRender render, final @Nullable RenderProgress old) {
        if (!Config.PROGRESS_LOGGING) {
            return null;
        }
        final Timer timer = new Timer("squaremap-render-progresslogger-[" + render.level.dimension().identifier() + "]");
        final RenderProgress renderProgress = new RenderProgress(render, old);
        timer.scheduleAtFixedRate(renderProgress, 1000L, 1000L);
        return Pair.of(timer, renderProgress);
    }

    @Override
    public void run() {
        if (this.render.mapWorld.renderManager().rendersPaused()) {
            return;
        }

        final int curChunks = this.render.processedChunks();
        final int diff = curChunks - this.prevChunks;
        this.prevChunks = curChunks;

        this.rollingAvgCps[this.rollingAvgIndex] = diff;
        this.totalAvgCps.add(diff);
        this.rollingAvgIndex++;
        if (this.rollingAvgIndex == ROLLING_AVG_SIZE) {
            this.rollingAvgIndex = 0;
        }
        final double rollingAvg = Arrays.stream(this.rollingAvgCps).filter(i -> i != 0).average().orElse(0.00D);

        final int chunksLeft = this.render.totalChunks() - curChunks;
        final long timeLeft = (long) (chunksLeft / (this.totalAvgCps.intStream().filter(i -> i != 0).average().orElse(0.00D) / 1000));

        String etaStr = formatMilliseconds(timeLeft);
        String elapsedStr = formatMilliseconds(System.currentTimeMillis() - this.startTime);

        double percent = (double) curChunks / (double) this.render.totalChunks();

        String rateStr = RATE_FORMAT.format(rollingAvg);
        String percentStr = PERCENT_FORMAT.format(percent);

        int curRegions = this.render.processedRegions();
        int totalRegions = this.render.totalRegions();

        if (this.seconds % Config.PROGRESS_LOGGING_INTERVAL != 0) {
            this.seconds++;
            return;
        }
        this.seconds++;

        Logging.info(
            (totalRegions > 0 ? Messages.LOG_RENDER_PROGRESS_WITH_REGIONS : Messages.LOG_RENDER_PROGRESS),
            "world", this.render.mapWorld.identifier().asString(),
            "current_regions", curRegions,
            "total_regions", totalRegions,
            "current_chunks", curChunks,
            "total_chunks", this.render.totalChunks(),
            "percent", percentStr,
            "elapsed", elapsedStr,
            "eta", etaStr,
            "rate", rateStr
        );
    }

    private static String formatMilliseconds(final long timeInMs) {
        final int hrs = (int) TimeUnit.MILLISECONDS.toHours(timeInMs);
        final int min = (int) TimeUnit.MILLISECONDS.toMinutes(timeInMs) % 60;
        final int sec = (int) TimeUnit.MILLISECONDS.toSeconds(timeInMs) % 60;
        return String.format("%02dh%02dm%02ds", hrs, min, sec);
    }
}
