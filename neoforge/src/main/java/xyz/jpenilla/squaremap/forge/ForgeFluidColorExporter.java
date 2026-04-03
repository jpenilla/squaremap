package xyz.jpenilla.squaremap.forge;

import com.google.inject.Inject;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.AbstractFluidColorExporter;

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
    protected int spritePixel(final TextureAtlasSprite sprite, final int x, final int y) {
        return sprite.getPixelRGBA(0, x, y);
    }
}
