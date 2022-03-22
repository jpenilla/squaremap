package xyz.jpenilla.squaremap.common.inject;

import com.google.inject.Module;
import java.util.List;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;

public interface ModulesConfiguration {
    ModulesConfiguration mapWorldFactory(Class<? extends MapWorldInternal.Factory<?>> mapWorldFactoryClass);

    ModulesConfiguration vanillaChunkSnapshotProvider();

    ModulesConfiguration vanillaRegionFileDirectoryResolver();

    ModulesConfiguration withModules(Module... modules);

    ModulesConfiguration withModule(Module module);

    List<Module> done();

    static ModulesConfiguration create(final SquaremapPlatform platform) {
        return new ModulesConfigurationImpl(platform);
    }
}
