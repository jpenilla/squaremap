package xyz.jpenilla.squaremap.forge.client;

import com.google.inject.Inject;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapDirectories;
import xyz.jpenilla.squaremap.common.client.AbstractFluidColorExporter;

@DefaultQualifier(NonNull.class)
public final class ForgeFluidColorExporter extends AbstractFluidColorExporter {
    @Inject
    private ForgeFluidColorExporter(final SquaremapDirectories directories) {
        super(directories);
    }

    @Override
    protected @Nullable Fluid fluid(final Block block) {
        if (block instanceof LiquidBlock liquidBlock) {
            return liquidBlock.fluid;
        }
        return null;
    }

    @Override
    protected int spritePixel(final TextureAtlasSprite sprite, final int x, final int y) {
        return sprite.getPixelARGB(0, x, y);
    }
}
