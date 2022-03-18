package xyz.jpenilla.squaremap.fabric.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.nio.file.Path;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.inject.annotation.DataDirectory;
import xyz.jpenilla.squaremap.fabric.SquaremapFabricInitializer;
import xyz.jpenilla.squaremap.fabric.command.FabricCommands;

@DefaultQualifier(NonNull.class)
public final class FabricModule extends AbstractModule {
    private final SquaremapFabricInitializer squaremapFabric;

    public FabricModule(final SquaremapFabricInitializer squaremapFabric) {
        this.squaremapFabric = squaremapFabric;
    }

    @Override
    protected void configure() {
        this.bind(SquaremapFabricInitializer.class)
            .toInstance(this.squaremapFabric);

        this.bind(PlatformCommands.class)
            .to(FabricCommands.class);

        this.bind(Path.class)
            .annotatedWith(DataDirectory.class)
            .toInstance(this.squaremapFabric.dataDirectory());
    }

    @Provides
    public ComponentFlattener componentFlattener(final SquaremapPlatform platform) {
        return FabricServerAudiences.of(((SquaremapFabricInitializer) platform).server()).flattener();
    }
}
