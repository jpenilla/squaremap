package xyz.jpenilla.squaremap.forge;

import com.google.inject.Inject;
import io.leangen.geantyref.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.IFluidBlock;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.jpenilla.squaremap.common.util.ColorBlender;
import xyz.jpenilla.squaremap.common.util.Colors;

@DefaultQualifier(NonNull.class)
public final class FluidColorExporter {
    @Inject
    private FluidColorExporter() {
    }

    public void export(final RegistryAccess registryAccess, final Path file) {
        final Map<String, String> map = new HashMap<>();
        registryAccess.registryOrThrow(Registries.BLOCK).holders().forEach(holder -> {
            final Block block = holder.value();
            final Fluid fluid;
            if (block instanceof IFluidBlock fluidBlock) {
                fluid = fluidBlock.getFluid();
            } else if (block instanceof LiquidBlock liquidBlock) {
                fluid = liquidBlock.getFluid();
            } else {
                return;
            }
            if (fluid == Fluids.WATER || fluid == Fluids.LAVA
                || fluid == Fluids.FLOWING_WATER || fluid == Fluids.FLOWING_LAVA) {
                return;
            }
            map.put(
                holder.key().location().toString(),
                Colors.toHexString(Colors.argbToRgba(color(fluid.getFluidType())))
            );
        });
        final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .path(file)
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions(options -> options.header("Automatically generated list of fluid colors. You may want to copy this into your server's advanced.yml."))
            .build();
        try {
            Files.createDirectories(file.getParent());
            final CommentedConfigurationNode node = loader.createNode();
            node.set(new TypeToken<>() {}, map);
            loader.save(node);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int color(final FluidType fluidType) {
        final IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluidType);
        final TextureAtlasSprite sprite = new Material(TextureAtlas.LOCATION_BLOCKS, ext.getStillTexture()).sprite();
        final ColorBlender blender = new ColorBlender();
        for (int i = 0; i < sprite.contents().width(); i++) {
            for (int h = 0; h < sprite.contents().height(); h++) {
                final int rgba = sprite.getPixelRGBA(0, i, h);
                blender.addColor(rgba);
            }
        }
        final int blended = Colors.fromNativeImage(blender.result());
        final int tint = ext.getTintColor();
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
