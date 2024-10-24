package xyz.jpenilla.squaremap.fabric;

import com.google.inject.Inject;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.AbstractFluidColorExporter;
import xyz.jpenilla.squaremap.common.util.ColorBlender;
import xyz.jpenilla.squaremap.common.util.Colors;
import xyz.jpenilla.squaremap.fabric.mixin.LiquidBlockAccess;

@DefaultQualifier(NonNull.class)
public final class FabricFluidColorExporter extends AbstractFluidColorExporter {
    @Inject
    private FabricFluidColorExporter(final DirectoryProvider directoryProvider) {
        super(directoryProvider);
    }

    @Override
    protected @Nullable Fluid fluid(final Block block) {
        if (block instanceof LiquidBlock liquidBlock) {
            return ((LiquidBlockAccess) liquidBlock).fluid();
        }
        return null;
    }

    @Override
    protected @Nullable String color(final Fluid fluid) {
        final @Nullable FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
        if (handler == null) {
            return null;
        }
        return Colors.toHexString(Colors.argbToRgba(color(fluid, handler)));
    }

    public static int color(final Fluid fluid, final FluidRenderHandler renderHandler) {
        final TextureAtlasSprite sprite = renderHandler.getFluidSprites(null, null, fluid.defaultFluidState())[0];
        final ColorBlender blender = new ColorBlender();
        for (int i = 0; i < sprite.contents().width(); i++) {
            for (int h = 0; h < sprite.contents().height(); h++) {
                final int rgba = ((SpriteContentsExtension) sprite.contents()).getPixel(i, h);
                blender.addColor(rgba);
            }
        }
        return color(
            blender.result(),
            renderHandler.getFluidColor(null, null, fluid.defaultFluidState())
        );
    }

    public interface SpriteContentsExtension {
        int getPixel(int x, int y);
    }
}
