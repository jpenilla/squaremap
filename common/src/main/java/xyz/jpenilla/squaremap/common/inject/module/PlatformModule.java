package xyz.jpenilla.squaremap.common.inject.module;

import com.google.inject.AbstractModule;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;

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
    }
}
