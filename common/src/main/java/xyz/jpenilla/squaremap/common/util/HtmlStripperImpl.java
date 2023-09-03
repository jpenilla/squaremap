package xyz.jpenilla.squaremap.common.util;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import xyz.jpenilla.squaremap.api.HtmlStripper;

@DefaultQualifier(NonNull.class)
public final class HtmlStripperImpl implements HtmlStripper {
    private static final PolicyFactory SANITIZER = new HtmlPolicyBuilder().toFactory();

    private HtmlStripperImpl() {
    }

    @Override
    public String stripHtml(final String string) {
        Objects.requireNonNull(string, "Parameter 'string' must not be null");
        return SANITIZER.sanitize(string);
    }

    public static final class Provider implements HtmlStripper.Provider {
        private static final HtmlStripper INSTANCE = new HtmlStripperImpl();

        @Override
        public HtmlStripper instance() {
            return INSTANCE;
        }
    }
}
