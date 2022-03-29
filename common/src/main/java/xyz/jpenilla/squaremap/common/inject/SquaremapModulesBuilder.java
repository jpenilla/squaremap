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

import static java.util.Objects.requireNonNull;

@DefaultQualifier(NonNull.class)
public final class SquaremapModulesBuilder {
    private final SquaremapPlatform platform;
    private final List<Module> extraModules = new ArrayList<>();
    private boolean vanillaRegionFileDirectoryResolver;
    private boolean vanillaChunkSnapshotProvider;
    private @Nullable Class<? extends MapWorldInternal.Factory<?>> mapWorldFactoryClass;

    private SquaremapModulesBuilder(final SquaremapPlatform platform) {
        this.platform = platform;
    }

    public SquaremapModulesBuilder mapWorldFactory(final Class<? extends MapWorldInternal.Factory<?>> mapWorldFactoryClass) {
        this.mapWorldFactoryClass = mapWorldFactoryClass;
        return this;
    }

    public SquaremapModulesBuilder vanillaChunkSnapshotProvider() {
        this.vanillaChunkSnapshotProvider = true;
        return this;
    }

    public SquaremapModulesBuilder vanillaRegionFileDirectoryResolver() {
        this.vanillaRegionFileDirectoryResolver = true;
        return this;
    }

    public SquaremapModulesBuilder withModules(final Module... modules) {
        this.extraModules.addAll(Arrays.asList(modules));
        return this;
    }

    public SquaremapModulesBuilder withModule(final Module module) {
        this.extraModules.add(module);
        return this;
    }

    public List<Module> build() {
        requireNonNull(this.mapWorldFactoryClass, "mapWorldFactoryClass");

        final List<Module> baseModules = List.of(
            new ApiModule(),
            new PlatformModule(this.platform),
            new FactoryModuleBuilder().build(RenderFactory.class),
            new FactoryModuleBuilder().build(this.mapWorldFactoryClass)
        );

        final List<Module> modules = new ArrayList<>(baseModules);

        if (this.vanillaChunkSnapshotProvider) {
            modules.add(new VanillaChunkSnapshotProviderModule());
        }

        if (this.vanillaRegionFileDirectoryResolver) {
            modules.add(new VanillaRegionFileDirectoryResolverModule());
        }

        modules.addAll(this.extraModules);
        return modules;
    }

    public static SquaremapModulesBuilder forPlatform(final SquaremapPlatform platform) {
        return new SquaremapModulesBuilder(platform);
    }
}
