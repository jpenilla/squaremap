package xyz.jpenilla.squaremap.forge.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.File;
import java.nio.file.Path;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.inject.annotation.DataDirectory;
import xyz.jpenilla.squaremap.forge.ForgeAdventure;
import xyz.jpenilla.squaremap.forge.ForgePlayerManager;
import xyz.jpenilla.squaremap.forge.ForgeServerAccess;
import xyz.jpenilla.squaremap.forge.SquaremapForge;
import xyz.jpenilla.squaremap.forge.command.ForgeCommands;

@DefaultQualifier(NonNull.class)
public final class ForgeModule extends AbstractModule {
    private final SquaremapForge squaremapForge;
    private final IEventBus modEventBus;
    private final ModContainer modContainer;

    public ForgeModule(
        final SquaremapForge squaremapForge,
        final IEventBus modEventBus,
        final ModContainer modContainer
    ) {
        this.squaremapForge = squaremapForge;
        this.modEventBus = modEventBus;
        this.modContainer = modContainer;
    }

    @Override
    protected void configure() {
        this.bind(SquaremapForge.class)
            .toInstance(this.squaremapForge);

        this.bind(PlatformCommands.class)
            .to(ForgeCommands.class);

        this.bind(ServerAccess.class)
            .to(ForgeServerAccess.class);

        this.bind(Path.class)
            .annotatedWith(DataDirectory.class)
            .toInstance(new File("squaremap").toPath());

        this.bind(AbstractPlayerManager.class)
            .to(ForgePlayerManager.class);

        this.bind(ModContainer.class)
            .toInstance(this.modContainer);

        this.bind(IEventBus.class)
            .toInstance(this.modEventBus);
    }

    @Provides
    public ComponentFlattener componentFlattener() {
        return ForgeAdventure.FLATTENER.get();
    }
}
