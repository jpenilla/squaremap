package net.pl3x.map.plugin.task.render;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Logging;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class RenderProgress extends TimerTask {

    private final DecimalFormat dfPercent = new DecimalFormat("0.00%");
    private final DecimalFormat dfRate = new DecimalFormat("0.0");
    private final AbstractRender render;
    private final long startTime;

    final int[] rollingAvgCps = new int[15];
    final List<Integer> totalAvgCps = new ArrayList<>();
    int index = 0;
    int prevChunks;
    long seconds;

    private RenderProgress(final @NonNull AbstractRender render) {
        this.startTime = System.currentTimeMillis();
        this.render = render;
        this.prevChunks = this.render.processedChunks();
    }

    public static @Nullable Timer printProgress(final @NonNull AbstractRender render) {
        if (!Config.PROGRESS_LOGGING) {
            return null;
        }
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new RenderProgress(render), 1000L, 1000L);
        return timer;
    }

    @Override
    public void run() {
        if (render.mapWorld.rendersPaused()) {
            return;
        }

        final int curChunks = this.render.processedChunks();
        final int diff = curChunks - this.prevChunks;
        this.prevChunks = curChunks;

        this.rollingAvgCps[this.index] = diff;
        this.totalAvgCps.add(diff);
        this.index++;
        if (this.index == 15) {
            this.index = 0;
        }
        final double rollingAvg = Arrays.stream(this.rollingAvgCps).filter(i -> i != 0).average().orElse(0.00D);

        final int chunksLeft = this.render.totalChunks() - curChunks;
        final long timeLeft = (long) (chunksLeft / (this.totalAvgCps.stream().filter(i -> i != 0).mapToInt(i -> i).average().orElse(0.00D) / 1000));

        String etaStr = formatMilliseconds(timeLeft);
        String elapsedStr = formatMilliseconds(System.currentTimeMillis() - this.startTime);

        double percent = (double) curChunks / (double) this.render.totalChunks();

        String rateStr = this.dfRate.format(rollingAvg);
        String percentStr = this.dfPercent.format(percent);

        int curRegions = this.render.processedRegions();
        int totalRegions = this.render.totalRegions();

        if (this.seconds % Config.PROGRESS_LOGGING_INTERVAL != 0) {
            this.seconds++;
            return;
        }
        this.seconds++;

        Logging.info(
            (totalRegions > 0 ? Lang.LOG_RENDER_PROGRESS_WITH_REGIONS : Lang.LOG_RENDER_PROGRESS),
            Template.template("world", render.world.getName()),
            Template.template("current_regions", Integer.toString(curRegions)),
            Template.template("total_regions", Integer.toString(totalRegions)),
            Template.template("current_chunks", Integer.toString(curChunks)),
            Template.template("total_chunks", Integer.toString(this.render.totalChunks())),
            Template.template("percent", percentStr),
            Template.template("elapsed", elapsedStr),
            Template.template("eta", etaStr),
            Template.template("rate", rateStr)
        );
    }

    private static @NonNull String formatMilliseconds(long timeLeft) {
        int hrs = (int) TimeUnit.MILLISECONDS.toHours(timeLeft) % 24;
        int min = (int) TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60;
        int sec = (int) TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60;
        return String.format("%02d:%02d:%02d", hrs, min, sec);
    }
}
