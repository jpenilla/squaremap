package xyz.jpenilla.squaremap.common.util;

import io.leangen.geantyref.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;

@DefaultQualifier(NonNull.class)
public abstract class AbstractFluidColorExporter {
    private final DirectoryProvider directoryProvider;

    protected AbstractFluidColorExporter(final DirectoryProvider directoryProvider) {
        this.directoryProvider = directoryProvider;
    }

    public final void export(final RegistryAccess registryAccess) {
        final Map<String, String> map = new HashMap<>();
        registryAccess.registryOrThrow(Registries.BLOCK).holders().forEach(holder -> {
            final Block block = holder.value();
            final @Nullable Fluid fluid = this.fluid(block);
            if (fluid == null
                || fluid == Fluids.WATER || fluid == Fluids.LAVA
                || fluid == Fluids.FLOWING_WATER || fluid == Fluids.FLOWING_LAVA) {
                return;
            }
            final @Nullable String color = this.color(fluid);
            if (color != null) {
                map.put(holder.key().location().toString(), color);
            }
        });

        final Path file = this.directoryProvider.dataDirectory().resolve("fluids-export.yml");
        final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .path(file)
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions(options -> options.header("Automatically generated list of fluid colors. You may want to copy these into your server's advanced.yml as block color overrides. See "))
            .build();
        try {
            Files.createDirectories(file.getParent());
            final CommentedConfigurationNode node = loader.createNode();
            node.set(new TypeToken<>() {}, map);
            loader.save(node);
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to write fluid color export to " + file, ex);
        }
    }

    protected abstract @Nullable Fluid fluid(Block block);

    protected abstract @Nullable String color(Fluid fluid);

    protected static int color(final int blended, final int tint) {
        final int tintA = tint >> 24 & 0xFF;
        final int blendedA = blended >> 24 & 0xFF;

        if (tint == 0xFFFFFFFF) {
            // no tint
            return blended;
        } else if (Colors.removeAlpha(tint) == 0xFFFFFFFF) {
            // alpha-only tint
            final int r = blended >> 16 & 0xFF;
            final int g = blended >> 8 & 0xFF;
            final int b = blended & 0xFF;
            return tintA << 24 | r << 16 | g << 8 | b;
        }

        // uses tint; pick lower alpha and tint rgb

        final int a = Math.min(tintA, blendedA);
        float r = (blended >> 16 & 0xFF) / 255f;
        float g = (blended >> 8 & 0xFF) / 255f;
        float b = (blended & 0xFF) / 255f;
        r *= tint >> 16 & 0xFF;
        g *= tint >> 8 & 0xFF;
        b *= tint & 0xFF;
        return a << 24 | (int) r << 16 | (int) g << 8 | (int) b;
    }

}
