package xyz.jpenilla.squaremap.fabric.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.jpenilla.squaremap.fabric.FabricFluidColorExporter;

@Mixin(SpriteContents.class)
abstract class SpriteContentsMixin implements FabricFluidColorExporter.SpriteContentsExtension {
    @Shadow @Final private NativeImage originalImage;

    @Override
    public int getPixelRGBA(final int x, final int y) {
        // always gets from frame 0 of animated texture
        return this.originalImage.getPixelRGBA(x, y);
    }
}
