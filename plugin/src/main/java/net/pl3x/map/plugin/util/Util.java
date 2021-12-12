package net.pl3x.map.plugin.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.plugin.util.NamedThreadFactory;

@DefaultQualifier(NonNull.class)
public final class Util {
    private Util() {
    }

    public static ThreadFactory squareMapThreadFactory(final String name) {
        return new NamedThreadFactory("squaremap-" + name);
    }

    public static ThreadFactory squareMapThreadFactory(final String name, final ServerLevel level) {
        return squareMapThreadFactory(name + "-[" + level.dimension().location() + "]");
    }

    public static void shutdownExecutor(final ExecutorService service, final TimeUnit timeoutUnit, final long timeoutLength) {
        service.shutdown();
        boolean didShutdown;
        try {
            didShutdown = service.awaitTermination(timeoutLength, timeoutUnit);
        } catch (final InterruptedException ignore) {
            didShutdown = false;
        }
        if (!didShutdown) {
            service.shutdownNow();
        }
    }
}
