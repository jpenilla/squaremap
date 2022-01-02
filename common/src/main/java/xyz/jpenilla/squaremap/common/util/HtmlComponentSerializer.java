package xyz.jpenilla.squaremap.common.util;

import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface HtmlComponentSerializer {
    static HtmlComponentSerializer withFlattener(ComponentFlattener flattener) {
        return new HtmlComponentSerializerImpl(flattener);
    }

    String serialize(ComponentLike componentLike);
}
