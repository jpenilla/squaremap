package xyz.jpenilla.squaremap.common.config;

import xyz.jpenilla.squaremap.common.util.ReflectionUtil;

@SuppressWarnings("unused")
public final class Advanced extends AbstractConfig {
    private Advanced() {
        super("advanced.yml");
    }

    static Advanced config;
    static int version;

    public static void reload() {
        config = new Advanced();

        version = config.getInt("config-version", 1);
        config.set("config-version", 1);

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

        WorldAdvanced.reload();
    }

    public static Advanced config() {
        return config;
    }
}
