package xyz.jpenilla.squaremap.sponge.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.nio.file.Path;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.adventure.SpongeComponents;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.inject.annotation.DataDirectory;
import xyz.jpenilla.squaremap.sponge.SpongePlayerManager;
import xyz.jpenilla.squaremap.sponge.SpongeServerAccess;
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

        this.bind(ServerAccess.class)
            .to(SpongeServerAccess.class);

        this.bind(PlatformCommands.class)
            .to(SpongeCommands.class);

        this.bind(AbstractPlayerManager.class)
            .to(SpongePlayerManager.class);
    }

    @Provides
    public ComponentFlattener componentFlattener() {
        return SpongeComponents.flattener();
    }
}
