package net.pl3x.map.plugin.task;

import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.configuration.Lang;

import java.text.DecimalFormat;

public class Progress extends Thread {
    private final DecimalFormat df = new DecimalFormat("##0.00%");
    private final long total;
    private long current;

    public Progress(long total) {
        this.total = total;
    }

    @Override
    public void run() {
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                logProgress();
                //noinspection BusyWait
                sleep(1000);
            }
        } catch (InterruptedException ignore) {
        }
    }

    @Override
    public void interrupt() {
        logProgress();
        super.interrupt();
    }

    public String progress() {
        return String.format("%1$7s", df.format((double) current / total));
    }

    public void logProgress() {
        Logger.info(Lang.LOG_JARLOADER_PROGRESS
                .replace("{percent}", progress())
                .replace("{current}", Long.toString(getCurrent()))
                .replace("{total}", Long.toString(getTotal())));
    }

    public long getCurrent() {
        return current;
    }

    public long getTotal() {
        return total;
    }

    public void add(long current) {
        this.current += current;
    }
}
