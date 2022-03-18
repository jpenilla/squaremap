package xyz.jpenilla.squaremap.common.inject;

import com.google.inject.AbstractModule;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.PlayerManager;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.WorldManager;

@DefaultQualifier(NonNull.class)
public final class PlatformModule extends AbstractModule {
    private final SquaremapPlatform platform;

    public PlatformModule(final SquaremapPlatform platform) {
        this.platform = platform;
    }

    @Override
    protected void configure() {
        this.bind(SquaremapPlatform.class)
            .toInstance(this.platform);

        this.bind(WorldManager.class)
            .toProvider(this.platform::worldManager);
        this.bind(PlayerManager.class)
            .toProvider(this.platform::playerManager);
        this.bind(AbstractPlayerManager.class)
            .toProvider(this.platform::playerManager);
    }
}
