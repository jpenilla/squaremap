package xyz.jpenilla.squaremap.common.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.kyori.adventure.text.minimessage.template.TemplateResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;

@DefaultQualifier(NonNull.class)
public final class Components {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.builder()
        .flattener(SquaremapCommon.instance().platform().componentFlattener())
        .build();

    public static PlainTextComponentSerializer plainSerializer() {
        return PLAIN_SERIALIZER;
    }

    public static MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }

    public static Component miniMessage(final String miniMessage) {
        return miniMessage().deserialize(miniMessage);
    }

    public static Component miniMessage(final String miniMessage, final Template... templates) {
        return miniMessage().deserialize(miniMessage, TemplateResolver.templates(templates));
    }
}
