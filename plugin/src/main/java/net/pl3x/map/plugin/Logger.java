package net.pl3x.map.plugin;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.configuration.Config;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.logging.Level;

public class Logger {
    public static java.util.logging.Logger log() {
        return Pl3xMapPlugin.getInstance().getLogger();
    }

    public static void debug(String msg) {
        if (Config.DEBUG_MODE) {
            info("<yellow>[DEBUG]</yellow> " + msg);
        }
    }

    public static void warn(String msg) {
        log().warning(msg);
    }

    public static void severe(String msg) {
        log().severe(msg);
    }

    public static void warn(String msg, Throwable t) {
        log().log(Level.WARNING, msg, t);
    }

    public static void severe(String msg, Throwable t) {
        log().log(Level.SEVERE, msg, t);
    }

    public static void info(final @NonNull String miniMessage, final @NonNull Template @NonNull ... placeholders) {
        Pl3xMapPlugin.getInstance().audiences().console().sendMessage(MiniMessage.get().parse(
                "<white>[<gray>Pl3xMap</gray>]</white> " + miniMessage,
                placeholders
        ));
    }

}
