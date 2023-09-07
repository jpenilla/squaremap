package xyz.jpenilla.squaremap.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Strips HTML tags from text. Allows sanitizing untrusted strings before use in
 * marker tooltips.
 *
 * @since 1.2.0
 */
@DefaultQualifier(NonNull.class)
public interface HtmlStripper {

    /**
     * Get an {@link HtmlStripper}.
     *
     * @return HTML stripper
     */
    static HtmlStripper htmlStripper() {
        return ProviderHolder.HTML_STRIPPER.instance();
    }

    /**
     * Strips HTML tags from the provided string.
     *
     * @param string untrusted string
     * @return sanitized string
     */
    String stripHtml(String string);

    @ApiStatus.Internal
    interface Provider {
        HtmlStripper instance();
    }

}
