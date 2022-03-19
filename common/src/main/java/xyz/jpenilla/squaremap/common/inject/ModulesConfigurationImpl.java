package xyz.jpenilla.squaremap.common.inject;

import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.inject.module.ApiModule;
import xyz.jpenilla.squaremap.common.inject.module.PlatformModule;
import xyz.jpenilla.squaremap.common.inject.module.VanillaChunkSnapshotProviderModule;
import xyz.jpenilla.squaremap.common.inject.module.VanillaRegionFileDirectoryResolverModule;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;

@DefaultQualifier(NonNull.class)
final class ModulesConfigurationImpl implements ModulesConfiguration {
    private final SquaremapPlatform platform;
    private final List<Module> extraModules = new ArrayList<>();
    private boolean vanillaRegionFileDirectoryResolver;
    private boolean vanillaChunkSnapshotProvider;
    private @Nullable Class<? extends MapWorldInternal.Factory<?>> mapWorldFactoryClass;

    public ModulesConfigurationImpl(final SquaremapPlatform platform) {
        this.platform = platform;
    }

    @Override
    public ModulesConfiguration mapWorldFactory(final Class<? extends MapWorldInternal.Factory<?>> mapWorldFactoryClass) {
        this.mapWorldFactoryClass = mapWorldFactoryClass;
        return this;
    }

    @Override
    public ModulesConfiguration vanillaChunkSnapshotProvider() {
        this.vanillaChunkSnapshotProvider = true;
        return this;
    }

    @Override
    public ModulesConfiguration vanillaRegionFileDirectoryResolver() {
        this.vanillaRegionFileDirectoryResolver = true;
        return this;
    }

    @Override
    public ModulesConfiguration withModules(final Module... modules) {
        this.extraModules.addAll(Arrays.asList(modules));
        return this;
    }

    @Override
    public ModulesConfiguration withModule(final Module module) {
        this.extraModules.add(module);
        return this;
    }

    @Override
    public List<Module> done() {
        if (this.mapWorldFactoryClass == null) {
            throw new IllegalArgumentException("mapWorldFactoryClass is null");
        }

        final List<Module> list = new ArrayList<>(
            List.of(
                new ApiModule(),
                new PlatformModule(this.platform),
                new FactoryModuleBuilder().build(RenderFactory.class),
                new FactoryModuleBuilder().build(this.mapWorldFactoryClass)
            )
        );

        if (this.vanillaChunkSnapshotProvider) {
            list.add(new VanillaChunkSnapshotProviderModule());
        }

        if (this.vanillaRegionFileDirectoryResolver) {
            list.add(new VanillaRegionFileDirectoryResolverModule());
        }

        list.addAll(this.extraModules);
        return list;
    }
}
