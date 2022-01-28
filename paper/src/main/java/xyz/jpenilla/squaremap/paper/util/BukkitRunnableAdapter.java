package xyz.jpenilla.squaremap.paper.util;

import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class BukkitRunnableAdapter extends BukkitRunnable {
    private final Runnable wrapped;

    public BukkitRunnableAdapter(final Runnable wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void run() {
        this.wrapped.run();
    }
}
