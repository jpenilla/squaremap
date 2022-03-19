package xyz.jpenilla.squaremap.common.inject.module;

import com.google.inject.AbstractModule;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.RegionFileDirectoryResolver;

@DefaultQualifier(NonNull.class)
public final class VanillaRegionFileDirectoryResolverModule extends AbstractModule {
    @Override
    protected void configure() {
        this.bind(RegionFileDirectoryResolver.class)
            .to(RegionFileDirectoryResolver.Vanilla.class);
    }
}
