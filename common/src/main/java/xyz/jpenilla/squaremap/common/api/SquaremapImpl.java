package xyz.jpenilla.squaremap.common.api;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.HtmlComponentSerializer;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.PlayerManager;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.SquaremapDirectories;
import xyz.jpenilla.squaremap.common.player.AbstractPlayerManager;
import xyz.jpenilla.squaremap.common.world.WorldManager;

@DefaultQualifier(NonNull.class)
@Singleton
public final class SquaremapImpl implements Squaremap {
    private final SquaremapDirectories directories;
    private final PlayerManager playerManager;
    private final WorldManager worldManager;
    private final IconRegistry iconRegistry;
    private final Provider<ComponentFlattener> flattener;

    @Inject
    private SquaremapImpl(
        final SquaremapDirectories directories,
        final AbstractPlayerManager playerManager,
        final WorldManager worldManager,
        final Provider<ComponentFlattener> flattener
    ) {
        this.directories = directories;
        this.playerManager = playerManager;
        this.worldManager = worldManager;
        this.iconRegistry = new IconRegistry(directories);
        this.flattener = flattener;
    }

    @Override
    public Collection<MapWorld> mapWorlds() {
        return Collections.unmodifiableCollection(this.worldManager.worlds());
    }

    @Override
    public Optional<MapWorld> getWorldIfEnabled(final WorldIdentifier identifier) {
        return this.worldManager.getWorldIfEnabled(identifier).map(Function.identity());
    }

    @Override
    public Registry<BufferedImage> iconRegistry() {
        return this.iconRegistry;
    }

    @Override
    public PlayerManager playerManager() {
        return this.playerManager;
    }

    @Override
    public Path webDir() {
        return this.directories.webDirectory();
    }

    @Override
    public HtmlComponentSerializer htmlComponentSerializer() {
        return HtmlComponentSerializer.withFlattener(this.flattener.get());
    }
}
