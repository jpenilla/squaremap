package xyz.jpenilla.squaremap.fabric;

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
    protected int spritePixel(final TextureAtlasSprite sprite, final int x, final int y) {
        return ((SpriteContentsExtension) sprite.contents()).getPixel(x, y);
    }

    public interface SpriteContentsExtension {
        int getPixel(int x, int y);
    }
}
