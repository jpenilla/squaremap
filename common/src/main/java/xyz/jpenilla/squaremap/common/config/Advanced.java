package xyz.jpenilla.squaremap.common.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import xyz.jpenilla.squaremap.common.SquaremapDirectories;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;

@SuppressWarnings("unused")
public final class Advanced extends AbstractConfig {
    private static final int LATEST_VERSION = 6;

    Advanced(final SquaremapDirectories directories) {
        super(directories.dataDirectory(), Advanced.class, "advanced.yml", LATEST_VERSION);
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
        final ConfigurationTransformation fourToFive = ConfigurationTransformation.builder()
            .addAction(
                defaultColorOverridesPath.withAppendedChild("blocks"),
                Transformations.modifyStringMap(map -> {
                    map.putIfAbsent("minecraft:wildflowers", "#EDD575");
                    map.putIfAbsent("minecraft:golden_dandelion", "#DBA213");
                    map.putIfAbsent("minecraft:torchflower", "#F6B927");
                    map.putIfAbsent("minecraft:pitcher_plant", "#6F6CCC");
                }))
            .build();
        final ConfigurationTransformation fiveToSix = ConfigurationTransformation.builder()
            .addAction(NodePath.path("world-settings"), Transformations.eachMapChild(worldSection -> {
                // Bush is grass-like ground cover, so hide it wherever short grass is still hidden.
                final ConfigurationNode invisibleBlocks = worldSection.node("invisible-blocks");
                final List<String> blocks = new ArrayList<>(invisibleBlocks.getList(String.class, List.of()));
                if (blocks.stream().anyMatch(Transformations.maybeMinecraft("short_grass")::contains)
                    && blocks.stream().noneMatch(Transformations.maybeMinecraft("bush")::contains)) {
                    blocks.add("minecraft:bush");
                    invisibleBlocks.setList(String.class, blocks);
                }
            }))
            .build();

        versionedBuilder.addVersion(2, oneToTwo);
        versionedBuilder.addVersion(3, twoToThree);
        versionedBuilder.addVersion(4, threeToFour);
        versionedBuilder.addVersion(5, fourToFive);
        versionedBuilder.addVersion(LATEST_VERSION, fiveToSix);
    }

    static Advanced config;

    public static void reload(final SquaremapDirectories directories) {
        config = new Advanced(directories);
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
