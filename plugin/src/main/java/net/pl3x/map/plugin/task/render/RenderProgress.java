package net.pl3x.map.plugin.task.render;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import net.pl3x.map.api.Pair;
import net.pl3x.map.plugin.Logging;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.minimessage.Template.template;

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

    public static @Nullable Pair<Timer, RenderProgress> printProgress(final AbstractRender render, final @Nullable RenderProgress old) {
        if (!Config.PROGRESS_LOGGING) {
            return null;
        }
        final Timer timer = new Timer("squaremap-render-progresslogger-[" + render.level.dimension().location() + "]");
        final RenderProgress renderProgress = new RenderProgress(render, old);
        timer.scheduleAtFixedRate(renderProgress, 1000L, 1000L);
        return Pair.of(timer, renderProgress);
    }

    @Override
    public void run() {
        if (this.render.mapWorld.rendersPaused()) {
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
            (totalRegions > 0 ? Lang.LOG_RENDER_PROGRESS_WITH_REGIONS : Lang.LOG_RENDER_PROGRESS),
            template("world", this.render.world.getName()),
            template("current_regions", text(curRegions)),
            template("total_regions", text(totalRegions)),
            template("current_chunks", text(curChunks)),
            template("total_chunks", text(this.render.totalChunks())),
            template("percent", percentStr),
            template("elapsed", elapsedStr),
            template("eta", etaStr),
            template("rate", rateStr)
        );
    }

    private static String formatMilliseconds(final long timeLeft) {
        final int hrs = (int) TimeUnit.MILLISECONDS.toHours(timeLeft) % 24;
        final int min = (int) TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60;
        final int sec = (int) TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60;
        return String.format("%02d:%02d:%02d", hrs, min, sec);
    }
}
