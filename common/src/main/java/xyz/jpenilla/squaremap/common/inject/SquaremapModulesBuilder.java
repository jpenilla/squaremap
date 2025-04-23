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
import xyz.jpenilla.squaremap.common.WorldManagerImpl;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.inject.module.ApiModule;
import xyz.jpenilla.squaremap.common.inject.module.PlatformModule;
import xyz.jpenilla.squaremap.common.inject.module.VanillaChunkSnapshotProviderFactoryModule;
import xyz.jpenilla.squaremap.common.inject.module.VanillaRegionFileDirectoryResolverModule;
import xyz.jpenilla.squaremap.common.task.TaskFactory;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;
import xyz.jpenilla.squaremap.common.util.EntityScheduler;
import xyz.jpenilla.squaremap.common.util.SquaremapJarAccess;

import static java.util.Objects.requireNonNull;

@DefaultQualifier(NonNull.class)
public final class SquaremapModulesBuilder {
    private final @Nullable SquaremapPlatform platform;
    private final @Nullable Class<? extends SquaremapPlatform> platformClass;
    private final List<Module> extraModules = new ArrayList<>();
    private boolean vanillaRegionFileDirectoryResolver;
    private boolean vanillaChunkSnapshotProviderFactory;
    private @Nullable Class<? extends MapWorldInternal> mapWorldClass;
    private Class<? extends SquaremapJarAccess> squaremapJarAccess = SquaremapJarAccess.JarFromCodeSource.class;
    private Class<? extends EntityScheduler> entitySchedulerClass = EntityScheduler.NoneEntityScheduler.class;
    private Class<? extends WorldManagerImpl> worldManagerClass = WorldManagerImpl.class;

    private SquaremapModulesBuilder(final SquaremapPlatform platform) {
        this.platform = platform;
        this.platformClass = null;
    }

    private SquaremapModulesBuilder(final Class<? extends SquaremapPlatform> platformClass) {
        this.platform = null;
        this.platformClass = platformClass;
    }

    public SquaremapModulesBuilder worldManager(final Class<? extends WorldManagerImpl> worldManagerClass) {
        this.worldManagerClass = worldManagerClass;
        return this;
    }

    public SquaremapModulesBuilder mapWorld(final Class<? extends MapWorldInternal> mapWorldClass) {
        this.mapWorldClass = mapWorldClass;
        return this;
    }

    public SquaremapModulesBuilder vanillaChunkSnapshotProviderFactory() {
        this.vanillaChunkSnapshotProviderFactory = true;
        return this;
    }

    public SquaremapModulesBuilder vanillaRegionFileDirectoryResolver() {
        this.vanillaRegionFileDirectoryResolver = true;
        return this;
    }

    public SquaremapModulesBuilder entityScheduler(final Class<? extends EntityScheduler> entitySchedulerClass) {
        this.entitySchedulerClass = entitySchedulerClass;
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

    public SquaremapModulesBuilder squaremapJarAccess(final Class<? extends SquaremapJarAccess> squaremapJarAccess) {
        this.squaremapJarAccess = squaremapJarAccess;
        return this;
    }

    public List<Module> build() {
        requireNonNull(this.mapWorldClass, "mapWorldClass");

        final List<Module> baseModules = List.of(
            new ApiModule(),
            new PlatformModule(this.platform, this.platformClass, this.squaremapJarAccess, this.entitySchedulerClass, this.worldManagerClass),
            new FactoryModuleBuilder().build(RenderFactory.class),
            new FactoryModuleBuilder().implement(MapWorldInternal.class, this.mapWorldClass).build(MapWorldInternal.Factory.class),
            new FactoryModuleBuilder().build(TaskFactory.class)
        );

        final List<Module> modules = new ArrayList<>(baseModules);

        if (this.vanillaChunkSnapshotProviderFactory) {
            modules.add(new VanillaChunkSnapshotProviderFactoryModule());
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

    public static SquaremapModulesBuilder forPlatform(final Class<? extends SquaremapPlatform> platformClass) {
        return new SquaremapModulesBuilder(platformClass);
    }
}
