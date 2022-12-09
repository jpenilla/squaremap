package xyz.jpenilla.squaremap.common.inject.module;

import com.google.inject.AbstractModule;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.util.SquaremapJarAccess;

@DefaultQualifier(NonNull.class)
public final class PlatformModule extends AbstractModule {
    private final SquaremapPlatform platform;
    private final Class<? extends SquaremapJarAccess> jarAccess;

    public PlatformModule(final SquaremapPlatform platform, final Class<? extends SquaremapJarAccess> jarAccess) {
        this.platform = platform;
        this.jarAccess = jarAccess;
    }

    @Override
    protected void configure() {
        this.bind(SquaremapPlatform.class)
            .toInstance(this.platform);
        this.bind(SquaremapJarAccess.class)
            .to(this.jarAccess);
    }
}
