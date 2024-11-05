package xyz.jpenilla.squaremap.common.config;

import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.transformation.TransformAction;
import xyz.jpenilla.squaremap.common.util.CheckedConsumer;

@DefaultQualifier(NonNull.class)
final class Transformations {
    private Transformations() {
    }

    static Set<String> maybeMinecraft(final String name) {
        return Set.of(name, "minecraft:" + name);
    }

    static TransformAction eachMapChild(final CheckedConsumer<ConfigurationNode, ConfigurateException> action) {
        return (path, node) -> {
            final Set<Object> childKeys = node.childrenMap().keySet();
            for (final Object childKey : childKeys) {
                final ConfigurationNode childNode = node.node(childKey);
                action.accept(childNode);
            }
            return null;
        };
    }

    static TransformAction modifyStringMap(
        final Consumer<Map<String, String>> consumer
    ) {
        return modifyMap(String.class, String.class, consumer);
    }

    @SuppressWarnings("unchecked")
    static <K, V> TransformAction modifyMap(
        final Class<K> keyType,
        final Class<V> valueType,
        final Consumer<Map<K, V>> consumer
    ) {
        return (path, node) -> {
            final Type type = TypeFactory.parameterizedClass(Map.class, keyType, valueType);
            final Map<K, V> map = (Map<K, V>) node.get(type, new HashMap<>());
            consumer.accept(map);
            node.set(type, map);
            return null;
        };
    }

    // expects nodes to be round-trip representable as List<String> or Map<String, String>
    static void applyMapKeyOrListValueRenames(final List<ConfigurationNode> sections, final Map<Set<String>, String> renames) throws SerializationException {
        for (final ConfigurationNode sectionNode : sections) {
            if (sectionNode.isList()) {
                final List<String> list = Objects.requireNonNull(sectionNode.getList(String.class));
                boolean anyRemoved = false;
                for (final Map.Entry<Set<String>, String> renameEntry : renames.entrySet()) {
                    boolean removed = false;
                    for (final String from : renameEntry.getKey()) {
                        removed |= list.remove(from);
                    }
                    if (removed) {
                        list.add(renameEntry.getValue());
                    }
                    anyRemoved |= removed;
                }
                if (anyRemoved) {
                    sectionNode.setList(String.class, list);
                }
            } else if (sectionNode.isMap()) {
                final TypeToken<Map<String, String>> type = new TypeToken<>() {};
                final Map<String, String> map = Objects.requireNonNull(sectionNode.get(type));
                boolean anyRemoved = false;
                for (final Map.Entry<Set<String>, String> renameEntry : renames.entrySet()) {
                    @Nullable String removed = null;
                    for (final String from : renameEntry.getKey()) {
                        final @Nullable String remove = map.remove(from);
                        if (remove != null) {
                            removed = remove;
                        }
                    }
                    if (removed != null) {
                        map.put(renameEntry.getValue(), removed);
                        anyRemoved = true;
                    }
                }
                if (anyRemoved) {
                    sectionNode.set(type, map);
                }
            }
        }
    }
}
