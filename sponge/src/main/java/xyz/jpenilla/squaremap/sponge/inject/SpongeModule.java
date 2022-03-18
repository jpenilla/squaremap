package xyz.jpenilla.squaremap.sponge.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.lang.reflect.Field;
import java.nio.file.Path;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.inject.annotation.DataDirectory;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;
import xyz.jpenilla.squaremap.common.util.Util;
import xyz.jpenilla.squaremap.sponge.command.SpongeCommands;

@DefaultQualifier(NonNull.class)
public final class SpongeModule extends AbstractModule {
    private final Path dataDirectory;

    public SpongeModule(final Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    @Override
    protected void configure() {
        this.bind(Path.class)
            .annotatedWith(DataDirectory.class)
            .toInstance(this.dataDirectory);

        this.bind(PlatformCommands.class)
            .to(SpongeCommands.class);
    }

    @Provides
    @Singleton
    public ComponentFlattener componentFlattener() {
        final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
        final Field flattener = ReflectionUtil.needField(plainText.getClass(), "flattener");
        try {
            return (ComponentFlattener) flattener.get(plainText);
        } catch (final IllegalAccessException ex) {
            throw Util.rethrow(ex);
        }
    }
}
