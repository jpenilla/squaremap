package xyz.jpenilla.squaremap.plugin;

import io.papermc.paper.text.PaperComponents;
import java.util.function.Supplier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.kyori.adventure.text.minimessage.template.TemplateResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.plugin.config.Config;

@DefaultQualifier(NonNull.class)
public final class Logging {
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.builder()
        .flattener(PaperComponents.flattener())
        .build();

    private Logging() {
    }

    public static Logger logger() {
        return SquaremapPlugin.getInstance().getLog4JLogger();
    }

    public static void debug(final Supplier<String> msg) {
        if (Config.DEBUG_MODE) {
            logger().info("[DEBUG] " + msg.get());
        }
    }

    public static void debug(final String msg) {
        if (Config.DEBUG_MODE) {
            logger().info("[DEBUG] " + msg);
        }
    }

    public static void warn(String msg, Throwable t) {
        logger().warn(msg, t);
    }

    public static void severe(String msg) {
        logger().error(msg);
    }

    public static void severe(String msg, Throwable t) {
        logger().error(msg, t);
    }

    public static void info(final String miniMessage, final Template... placeholders) {
        logger().info(
            PLAIN_SERIALIZER.serialize(
                MiniMessage.miniMessage().deserialize(
                    miniMessage,
                    TemplateResolver.templates(placeholders)
                )
            )
        );
    }
}
