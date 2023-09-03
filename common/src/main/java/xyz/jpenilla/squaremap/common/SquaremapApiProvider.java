package xyz.jpenilla.squaremap.common;

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
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;

@DefaultQualifier(NonNull.class)
@Singleton
public final class SquaremapApiProvider implements Squaremap {
    private final DirectoryProvider directoryProvider;
    private final PlayerManager playerManager;
    private final WorldManager worldManager;
    private final IconRegistry iconRegistry;
    private final Provider<ComponentFlattener> flattener;

    @Inject
    private SquaremapApiProvider(
        final DirectoryProvider directoryProvider,
        final AbstractPlayerManager playerManager,
        final WorldManager worldManager,
        final Provider<ComponentFlattener> flattener
    ) {
        this.directoryProvider = directoryProvider;
        this.playerManager = playerManager;
        this.worldManager = worldManager;
        this.iconRegistry = new IconRegistry(directoryProvider);
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
        return this.directoryProvider.webDirectory();
    }

    @Override
    public HtmlComponentSerializer htmlComponentSerializer() {
        return HtmlComponentSerializer.withFlattener(this.flattener.get());
    }
}
