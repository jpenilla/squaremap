package xyz.jpenilla.squaremap.common;

import java.util.function.Supplier;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.Config;

@DefaultQualifier(NonNull.class)
public final class Logging {
    private Logging() {
    }

    public static Logger logger() {
        return SquaremapCommon.instance().platform().logger();
    }

    public static void debug(final Supplier<String> msg) {
        if (Config.DEBUG_MODE) {
            logger().info("[DEBUG] " + msg.get());
        }
    }

    public static void error(final String message, final Throwable thr, final Object... replacements) {
        logger().error(replace(message, replacements), thr);
    }

    public static void info(final String message, final Object... replacements) {
        logger().info(replace(message, replacements));
    }

    public static String replace(String message, final Object... replacements) {
        if (replacements.length == 0) {
            return message;
        }
        if ((replacements.length & 1) != 0) {
            throw new IllegalArgumentException("Invalid length for replacements array (expected to be divisible by 2)");
        }
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace('<' + replacements[i].toString() + '>', replacements[i + 1].toString());
        }
        return message;
    }
}
