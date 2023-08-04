package xyz.jpenilla.squaremap.sponge.mixin;

import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.jpenilla.squaremap.sponge.SquaremapSpongeBootstrap;

@Mixin(Main.class)
abstract class MainMixin {
    @Inject(
        method = "main",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/ServerPacksSource;createPackRepository(Ljava/nio/file/Path;)Lnet/minecraft/server/packs/repository/PackRepository;"
        )
    )
    private static void startSquaremap(final String[] $$0, final CallbackInfo ci) {
        // currently we want to init before command registration, but when it is safe to classload mc.
        // I couldn't find the right event for that, and didn't feel like refactoring things for Sponge,
        // so a Mixin will work for now
        SquaremapSpongeBootstrap.instance.init();
    }
}
