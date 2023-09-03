package xyz.jpenilla.squaremap.api;

import net.kyori.adventure.util.Services;

final class ProviderHolder {
    static final HtmlComponentSerializer.Provider HTML_SERIALIZER = service(HtmlComponentSerializer.Provider.class);
    static final HtmlStripper.Provider HTML_STRIPPER = service(HtmlStripper.Provider.class);

    private static <T> T service(final Class<T> clazz) {
        return Services.service(clazz)
            .orElseThrow(() -> new IllegalStateException("Could not find " + clazz.getName() + " implementation"));
    }
}
