package xyz.jpenilla.squaremap.common;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.PlayerManager;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.common.util.FileUtil;

public final class SquaremapApiProvider implements Squaremap {
    private final SquaremapPlatform platform;
    private final IconRegistry iconRegistry;

    public SquaremapApiProvider(final @NonNull SquaremapPlatform platform) {
        this.platform = platform;
        this.iconRegistry = new IconRegistry();
    }

    @Override
    public @NonNull Collection<MapWorld> mapWorlds() {
        return Collections.unmodifiableCollection(this.platform.worldManager().worlds().values());
    }

    @Override
    public @NonNull Optional<MapWorld> getWorldIfEnabled(@NonNull WorldIdentifier identifier) {
        return Optional.ofNullable(this.platform.level(identifier))
            .flatMap(w -> this.platform.worldManager().getWorldIfEnabled(w));
    }

    @Override
    public @NonNull Registry<BufferedImage> iconRegistry() {
        return this.iconRegistry;
    }

    @Override
    public @NonNull PlayerManager playerManager() {
        return this.platform.playerManager();
    }

    @Override
    public @NonNull Path webDir() {
        return FileUtil.WEB_DIR;
    }
}
