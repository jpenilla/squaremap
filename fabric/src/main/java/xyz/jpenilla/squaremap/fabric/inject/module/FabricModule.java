package xyz.jpenilla.squaremap.fabric.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.inject.annotation.DataDirectory;
import xyz.jpenilla.squaremap.fabric.FabricServerAccess;
import xyz.jpenilla.squaremap.fabric.SquaremapFabric;
import xyz.jpenilla.squaremap.fabric.command.FabricCommands;

@DefaultQualifier(NonNull.class)
public final class FabricModule extends AbstractModule {
    private final SquaremapFabric squaremapFabric;

    public FabricModule(final SquaremapFabric squaremapFabric) {
        this.squaremapFabric = squaremapFabric;
    }

    @Override
    protected void configure() {
        this.bind(SquaremapFabric.class)
            .toInstance(this.squaremapFabric);

        this.bind(PlatformCommands.class)
            .to(FabricCommands.class);

        this.bind(ServerAccess.class)
            .to(FabricServerAccess.class);

        this.bind(Path.class)
            .annotatedWith(DataDirectory.class)
            .toInstance(FabricLoader.getInstance().getGameDir().resolve("squaremap"));
    }

    @Provides
    public ComponentFlattener componentFlattener(final SquaremapFabric platform) {
        return FabricServerAudiences.of(platform.server()).flattener();
    }
}
