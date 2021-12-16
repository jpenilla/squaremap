package xyz.jpenilla.squaremap.plugin.util;

import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextFormat;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

@DefaultQualifier(NonNull.class)
public final class HtmlComponentSerializer {
    private static final PolicyFactory SANITIZER = Sanitizers.STYLES.and(Sanitizers.FORMATTING);

    private final ComponentFlattener flattener;

    private HtmlComponentSerializer(final ComponentFlattener flattener) {
        this.flattener = flattener;
    }

    public String serialize(final ComponentLike componentLike) {
        final HtmlFlattener state = new HtmlFlattener();
        this.flattener.flatten(componentLike.asComponent(), state);
        return SANITIZER.sanitize(state.toString());
    }

    public static HtmlComponentSerializer withFlattener(final ComponentFlattener flattener) {
        return new HtmlComponentSerializer(flattener);
    }

    private static final class HtmlFlattener implements FlattenerListener {
        private static final char[] OBFUSCATED_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        private static final String CLOSE_SPAN = "</span>";

        private final StringBuilder sb = new StringBuilder();
        private final ArrayDeque<Style> stack = new ArrayDeque<>();

        private HtmlFlattener() {
        }

        @Override
        public void pushStyle(final Style style) {
            this.stack.push(style);
        }

        @Override
        public void component(final String text) {
            final Style style = this.stack.stream()
                .reduce(Style.empty(), Style::merge);
            final int i = this.append(style);
            if (style.decorations().get(TextDecoration.OBFUSCATED) == TextDecoration.State.TRUE) {
                text.codePoints().forEach($ ->
                    this.sb.append(OBFUSCATED_CHARS[ThreadLocalRandom.current().nextInt(OBFUSCATED_CHARS.length)]));
            } else {
                this.sb.append(text);
            }
            this.close(i);
        }

        @Override
        public void popStyle(final Style style) {
            this.stack.removeLastOccurrence(style);
        }

        private void append(final TextFormat format) {
            this.sb.append(asHtml(format));
        }

        private void close(final int i) {
            for (int index = 0; index < i; index++) {
                this.sb.append(CLOSE_SPAN);
            }
        }

        private int append(final Style style) {
            final int[] opened = {0};
            final @Nullable TextColor color = style.color();
            if (color != null) {
                this.append(color);
                opened[0]++;
            }
            style.decorations().forEach((decoration, state) -> {
                if (decoration == TextDecoration.OBFUSCATED) {
                    return; // handled elsewhere
                }
                if (state == TextDecoration.State.TRUE) {
                    this.append(decoration);
                    opened[0]++;
                }
            });
            return opened[0];
        }

        public String toString() {
            return this.sb.toString();
        }

        private static String asHtml(final TextFormat format) {
            if (format instanceof TextColor textColor) {
                return "<span style='color:" + textColor.asHexString() + "'>";
            } else if (format == TextDecoration.OBFUSCATED) {
                return ""; // handled elsewhere
            } else if (format instanceof TextDecoration decoration) {
                final String inner = switch (decoration) {
                    case BOLD -> "font-weight:bold";
                    case ITALIC -> "font-style:italic";
                    case OBFUSCATED -> throw new IllegalStateException(); // needed to satisfy compiler
                    case UNDERLINED -> "text-decoration:underline";
                    case STRIKETHROUGH -> "text-decoration:line-through";
                };
                return "<span style='" + inner + "'>";
            }
            throw new IllegalArgumentException("Cannot handle format: " + format + " (" + format.getClass().getTypeName() + ")");
        }
    }
}
