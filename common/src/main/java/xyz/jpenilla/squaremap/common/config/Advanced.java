package xyz.jpenilla.squaremap.common.config;

import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;

@SuppressWarnings("unused")
public final class Advanced extends AbstractConfig {
    Advanced(final DirectoryProvider directoryProvider) {
        super(directoryProvider.dataDirectory(), Advanced.class, "advanced.yml", 1);
    }

    static Advanced config;

    public static void reload(
        final DirectoryProvider directoryProvider,
        final ServerAccess serverAccess
    ) {
        config = new Advanced(directoryProvider);
        config.readConfig(Advanced.class, null);

        // todo - replace hack
        final Class<?> bukkitAdvancedClass = ReflectionUtil.findClass("xyz.jpenilla.squaremap.paper.config.PaperAdvanced");
        if (bukkitAdvancedClass != null) {
            config.readConfig(bukkitAdvancedClass, null);
        }
        final Class<?> spongeAdvancedClass = ReflectionUtil.findClass("xyz.jpenilla.squaremap.sponge.config.SpongeAdvanced");
        if (spongeAdvancedClass != null) {
            config.readConfig(spongeAdvancedClass, null);
        }

        WorldAdvanced.reload(serverAccess);
    }

    public static Advanced config() {
        return config;
    }
}
