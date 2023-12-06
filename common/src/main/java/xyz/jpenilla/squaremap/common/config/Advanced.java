package xyz.jpenilla.squaremap.common.config;

import io.leangen.geantyref.TypeToken;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;

@SuppressWarnings("unused")
public final class Advanced extends AbstractConfig {
    private static final int LATEST_VERSION = 3;

    Advanced(final DirectoryProvider directoryProvider) {
        super(directoryProvider.dataDirectory(), Advanced.class, "advanced.yml", LATEST_VERSION);
    }

    @Override
    protected void addVersions(ConfigurationTransformation.VersionedBuilder versionedBuilder) {
        final ConfigurationTransformation oneToTwo = ConfigurationTransformation.builder()
            .addAction(NodePath.path("world-settings", "default", "color-overrides"), (path, node) -> {
                final TypeToken<Map<String, String>> type = new TypeToken<>() {};

                final ConfigurationNode foliageNode = node.node("biomes", "foliage");
                final Map<String, String> foliageMap = foliageNode.get(type, new HashMap<>());
                foliageMap.put("minecraft:mangrove_swamp", "#6f9623");
                foliageNode.set(type, foliageMap);

                final ConfigurationNode blocksNode = node.node("blocks");
                final Map<String, String> blocksMap = blocksNode.get(type, new HashMap<>());
                blocksMap.put("minecraft:pink_petals", "#FFB4DB");
                blocksNode.set(type, blocksMap);
                return null;
            })
            .build();
        final ConfigurationTransformation twoToThree = ConfigurationTransformation.builder()
            .addAction(NodePath.path("world-settings"), (path, node) -> {
                final Set<Object> childKeys = node.childrenMap().keySet();
                for (final Object childKey : childKeys) {
                    final ConfigurationNode worldSection = node.node(childKey);
                    final List<ConfigurationNode> sections = List.of(
                        worldSection.node("invisible-blocks"),
                        worldSection.node("iterate-up-base-blocks"),
                        worldSection.node("color-overrides", "blocks")
                    );
                    for (final ConfigurationNode sectionNode : sections) {
                        if (sectionNode.isList()) {
                            final List<String> list = Objects.requireNonNull(sectionNode.getList(String.class));
                            boolean removed = false;
                            removed |= list.remove("minecraft:grass");
                            removed |= list.remove("grass");
                            if (removed) {
                                list.add("minecraft:short_grass");
                                sectionNode.setList(String.class, list);
                            }
                        } else if (sectionNode.isMap()) {
                            final TypeToken<Map<String, String>> type = new TypeToken<>() {};
                            final Map<String, String> map = Objects.requireNonNull(sectionNode.get(type));
                            @Nullable String removed;
                            removed = map.remove("minecraft:grass");
                            if (removed == null) {
                                removed = map.remove("grass");
                            }
                            if (removed != null) {
                                map.put("minecraft:short_grass", removed);
                                sectionNode.set(type, map);
                            }
                        }
                    }
                }
                return null;
            })
            .build();

        versionedBuilder.addVersion(2, oneToTwo);
        versionedBuilder.addVersion(LATEST_VERSION, twoToThree);
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
