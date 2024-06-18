package xyz.jpenilla.squaremap.forge;

import com.google.inject.Inject;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.AbstractFluidColorExporter;
import xyz.jpenilla.squaremap.common.util.ColorBlender;
import xyz.jpenilla.squaremap.common.util.Colors;

@DefaultQualifier(NonNull.class)
public final class ForgeFluidColorExporter extends AbstractFluidColorExporter {
    @Inject
    private ForgeFluidColorExporter(final DirectoryProvider directoryProvider) {
        super(directoryProvider);
    }

    @Override
    protected @Nullable Fluid fluid(final Block block) {
        if (block instanceof LiquidBlock liquidBlock) {
            return liquidBlock.fluid;
        }
        return null;
    }

    @Override
    protected String color(final Fluid fluid) {
        return Colors.toHexString(Colors.argbToRgba(color(fluid.getFluidType())));
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
        return color(Colors.abgrToArgb(blender.result()), ext.getTintColor());
    }
}
