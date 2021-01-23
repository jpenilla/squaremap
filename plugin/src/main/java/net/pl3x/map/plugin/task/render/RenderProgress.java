package net.pl3x.map.plugin.task.render;

import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.configuration.Lang;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public final class RenderProgress extends TimerTask {

    private final DecimalFormat dfPercent = new DecimalFormat("0.00%");
    private final DecimalFormat dfRate = new DecimalFormat("0.0");
    private final AbstractRender render;
    private final long startTime;

    final int[] rollingAvgCps = new int[15];
    final List<Integer> totalAvgCps = new ArrayList<>();
    int index = 0;
    int prevChunks = 0;

    private RenderProgress(final @NonNull AbstractRender render) {
        this.startTime = System.currentTimeMillis();
        this.render = render;
    }

    public static Timer printProgress(final @NonNull AbstractRender render) {
        final RenderProgress progress = new RenderProgress(render);
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(progress, 1000L, 1000L);
        return timer;
    }

    @Override
    public void run() {
        final int curChunks = this.render.processedChunks();
        final int diff = curChunks - prevChunks;
        prevChunks = curChunks;

        rollingAvgCps[index] = diff;
        totalAvgCps.add(diff);
        index++;
        if (index == 15) {
            index = 0;
        }
        final double rollingAvg = Arrays.stream(rollingAvgCps).filter(i -> i != 0).average().orElse(0.00D);

        final int chunksLeft = this.render.totalChunks() - curChunks;
        final long timeLeft = (long) (chunksLeft / (totalAvgCps.stream().filter(i -> i != 0).mapToInt(i -> i).average().orElse(0.00D) / 1000));

        String etaStr = formatMilliseconds(timeLeft);
        String elapsedStr = formatMilliseconds(System.currentTimeMillis() - startTime);

        double percent = (double) curChunks / (double) this.render.totalChunks();

        String rateStr = dfRate.format(rollingAvg);
        String percentStr = dfPercent.format(percent);

        Logger.info(
                Lang.LOG_RENDER_PROGRESS,
                Template.of("world", render.world.getName()),
                Template.of("current_chunks", Integer.toString(curChunks)),
                Template.of("total_chunks", Integer.toString(this.render.totalChunks())),
                Template.of("percent", percentStr),
                Template.of("elapsed", elapsedStr),
                Template.of("eta", etaStr),
                Template.of("rate", rateStr)
        );

    }

    private static @NonNull String formatMilliseconds(long timeLeft) {
        int hrs = (int) TimeUnit.MILLISECONDS.toHours(timeLeft) % 24;
        int min = (int) TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60;
        int sec = (int) TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60;
        return String.format("%02d:%02d:%02d", hrs, min, sec);
    }
}
