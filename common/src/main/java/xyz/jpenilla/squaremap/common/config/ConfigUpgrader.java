package xyz.jpenilla.squaremap.common.config;

import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public final class ConfigUpgrader {
    private final ConfigurationTransformation.Versioned upgrader;

    public ConfigUpgrader(final Consumer<ConfigurationTransformation.VersionedBuilder> op) {
        final ConfigurationTransformation.VersionedBuilder builder = ConfigurationTransformation.versionedBuilder();
        op.accept(builder);
        this.upgrader = builder.build();
    }

    public <N extends ConfigurationNode> UpgradeResult<N> upgrade(final N node) {
        final int original = this.upgrader.version(node);
        try {
            this.upgrader.apply(node);
        } catch (final ConfigurateException e) {
            Util.rethrow(e);
        }
        final int newVer = this.upgrader.version(node);
        return new UpgradeResult<>(original, newVer, node, original != newVer);
    }

    public record UpgradeResult<N extends ConfigurationNode>(
        int originalVersion,
        int newVersion,
        N node,
        boolean didUpgrade
    ) {
    }
}
