package xyz.jpenilla.squaremap.sponge.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.nio.file.Path;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.Game;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.network.channel.ChannelManager;
import org.spongepowered.plugin.PluginContainer;
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
    private final PluginContainer pluginContainer;
    private final Game game;
    private final ChannelManager channelManager;

    @Inject
    private SpongeModule(
        @ConfigDir(sharedRoot = false) final Path dataDirectory,
        final PluginContainer pluginContainer,
        final Game game,
        final ChannelManager channelManager
    ) {
        this.dataDirectory = dataDirectory;
        this.pluginContainer = pluginContainer;
        this.game = game;
        this.channelManager = channelManager;
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

        this.bind(PluginContainer.class)
            .toInstance(this.pluginContainer);

        this.bind(Game.class)
            .toInstance(this.game);

        this.bind(ChannelManager.class)
            .toInstance(this.channelManager);
    }

    @Provides
    public ComponentFlattener componentFlattener() {
        return SpongeComponents.flattener();
    }
}
