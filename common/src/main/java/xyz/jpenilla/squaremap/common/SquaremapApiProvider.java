package xyz.jpenilla.squaremap.common;

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
import xyz.jpenilla.squaremap.common.util.FileUtil;

@DefaultQualifier(NonNull.class)
public final class SquaremapApiProvider implements Squaremap {
    private final SquaremapPlatform platform;
    private final IconRegistry iconRegistry;

    public SquaremapApiProvider(final SquaremapPlatform platform) {
        this.platform = platform;
        this.iconRegistry = new IconRegistry();
    }

    @Override
    public Collection<MapWorld> mapWorlds() {
        return Collections.unmodifiableCollection(this.platform.worldManager().worlds().values());
    }

    @Override
    public Optional<MapWorld> getWorldIfEnabled(final WorldIdentifier identifier) {
        return Optional.ofNullable(this.platform.level(identifier))
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
        return FileUtil.WEB_DIR;
    }
}
