package xyz.jpenilla.squaremap.common.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class Components {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }

    public static Component miniMessage(final String miniMessage) {
        return miniMessage().deserialize(miniMessage);
    }

    public static Component miniMessage(final String miniMessage, final TagResolver... templates) {
        return miniMessage().deserialize(miniMessage, TagResolver.resolver(templates));
    }
}
