package xyz.jpenilla.squaremap.common.config;

import java.util.List;
import java.util.Map;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;

@SuppressWarnings("unused")
public final class Advanced extends AbstractConfig {
    private static final int LATEST_VERSION = 4;

    Advanced(final DirectoryProvider directoryProvider) {
        super(directoryProvider.dataDirectory(), Advanced.class, "advanced.yml", LATEST_VERSION);
    }

    @Override
    protected void addVersions(ConfigurationTransformation.VersionedBuilder versionedBuilder) {
        final NodePath defaultColorOverridesPath = NodePath.path("world-settings", "default", "color-overrides");

        final ConfigurationTransformation oneToTwo = ConfigurationTransformation.builder()
            .addAction(
                defaultColorOverridesPath.withAppendedChild("biomes").withAppendedChild("foliage"),
                Transformations.modifyStringMap(map -> {
                    map.put("minecraft:mangrove_swamp", "#6f9623");
                })
            )
            .addAction(
                defaultColorOverridesPath.withAppendedChild("blocks"),
                Transformations.modifyStringMap(map -> {
                    map.put("minecraft:pink_petals", "#FFB4DB");
                })
            )
            .build();
        final ConfigurationTransformation twoToThree = ConfigurationTransformation.builder()
            .addAction(NodePath.path("world-settings"), Transformations.eachMapChild(worldSection -> {
                Transformations.applyMapKeyOrListValueRenames(
                    List.of(
                        worldSection.node("invisible-blocks"),
                        worldSection.node("iterate-up-base-blocks"),
                        worldSection.node("color-overrides", "blocks")
                    ),
                    Map.of(
                        Transformations.maybeMinecraft("grass"), "minecraft:short_grass"
                    )
                );
            }))
            .build();
        final ConfigurationTransformation threeToFour = ConfigurationTransformation.builder()
            .addAction(
                defaultColorOverridesPath.withAppendedChild("blocks"),
                Transformations.modifyStringMap(map -> {
                    map.put("minecraft:pale_oak_leaves", "#626760");
                }))
            .build();

        versionedBuilder.addVersion(2, oneToTwo);
        versionedBuilder.addVersion(3, twoToThree);
        versionedBuilder.addVersion(LATEST_VERSION, threeToFour);
    }

    static Advanced config;

    public static void reload(final DirectoryProvider directoryProvider) {
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
    }

    public static Advanced config() {
        return config;
    }
}
