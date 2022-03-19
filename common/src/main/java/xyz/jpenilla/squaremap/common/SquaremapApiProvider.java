package xyz.jpenilla.squaremap.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.PlayerManager;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;

@DefaultQualifier(NonNull.class)
@Singleton
public final class SquaremapApiProvider implements Squaremap {
    private final SquaremapPlatform platform;
    private final IconRegistry iconRegistry;
    private final ServerAccess serverAccess;
    private final DirectoryProvider directoryProvider;

    @Inject
    private SquaremapApiProvider(
        final SquaremapPlatform platform,
        final DirectoryProvider directoryProvider,
        final ServerAccess serverAccess
    ) {
        this.platform = platform;
        this.directoryProvider = directoryProvider;
        this.iconRegistry = new IconRegistry(directoryProvider);
        this.serverAccess = serverAccess;
    }

    @Override
    public Collection<MapWorld> mapWorlds() {
        return Collections.unmodifiableCollection(this.platform.worldManager().worlds().values());
    }

    @Override
    public Optional<MapWorld> getWorldIfEnabled(final WorldIdentifier identifier) {
        return Optional.ofNullable(this.serverAccess.level(identifier))
            .flatMap(w -> this.platform.worldManager().getWorldIfEnabled(w));
    }

    @Override
    public Registry<BufferedImage> iconRegistry() {
        return this.iconRegistry;
    }

    @Override
    public PlayerManager playerManager() {
        return this.platform.playerManager();
    }

    @Override
    public Path webDir() {
        return this.directoryProvider.webDirectory();
    }
}
