package xyz.jpenilla.squaremap.common.visibilitylimit;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import xyz.jpenilla.squaremap.api.Point;

public final class VisibilityShapeSerializer implements TypeSerializer<VisibilityShape> {

    @Override
    public VisibilityShape deserialize(final Type type, final ConfigurationNode node) throws SerializationException {
        final String typeString = node.node("type").getString();
        if (typeString == null) {
            throw new SerializationException(node.node("type"), String.class, "Missing type for visibility shape");
        }
        final @Nullable String enabled = node.node("enabled").getString();
        if (enabled != null && !Boolean.parseBoolean(enabled)) {
            return VisibilityShape.NULL;
        }
        return switch (typeString) {
            case "circle" -> this.parseCircleShape(node);
            case "rectangle" -> this.parseRectangleShape(node);
            case "polygon" -> this.parsePolygonShape(node);
            case "world-border" -> new WorldBorderShape();
            default -> throw new SerializationException(
                node.node("type"), String.class, "Unknown shape type '" + typeString + "'");
        };
    }

    private VisibilityShape parseCircleShape(final ConfigurationNode visibilityLimit) {
        final int centerX = visibilityLimit.node("center-x").getInt();
        final int centerZ = visibilityLimit.node("center-z").getInt();
        final int radius = visibilityLimit.node("radius").getInt();
        if (radius > 0) {
            return new CircleShape(centerX, centerZ, radius);
        }
        return null;
    }

    private VisibilityShape parseRectangleShape(final ConfigurationNode visibilityLimit) {
        final int minX = visibilityLimit.node("min-x").getInt();
        final int minZ = visibilityLimit.node("min-z").getInt();
        final int maxX = visibilityLimit.node("max-x").getInt();
        final int maxZ = visibilityLimit.node("max-z").getInt();
        if (maxX >= minX && maxZ >= minZ) {
            return new RectangleShape(new BlockPos(minX, 0, minZ), new BlockPos(maxX, 0, maxZ));
        }
        return null;
    }

    private VisibilityShape parsePolygonShape(final ConfigurationNode visibilityLimit) throws SerializationException {
        final List<String> pointStrings = visibilityLimit.node("points").getList(String.class);
        if (pointStrings == null) {
            throw new SerializationException("Missing point list");
        }
        return new PolygonShape(pointStrings.stream().map(s -> {
            final int[] ints = Arrays.stream(s.split("[, ]")).filter(Predicate.not(String::isBlank)).mapToInt(Integer::parseInt).toArray();
            return Point.of(ints[0], ints[1]);
        }).toList());
    }

    @Override
    public void serialize(final Type type, final @Nullable VisibilityShape obj, final ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.raw(null);
            return;
        }
        if (obj instanceof WorldBorderShape) {
            // writing default config
            node.node("type").set("world-border");
            node.node("enabled").set("false");
            return;
        }
        throw new UnsupportedOperationException();
    }
}
