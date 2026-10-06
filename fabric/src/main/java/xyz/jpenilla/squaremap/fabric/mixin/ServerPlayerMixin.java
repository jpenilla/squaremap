package xyz.jpenilla.squaremap.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.jpenilla.squaremap.fabric.player.FabricPlayerManager;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @Inject(
        method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V",
        at = @At("HEAD")
    )
    private void squaremap$migrateLegacyHidden(final ValueInput input, final CallbackInfo ci) {
        FabricPlayerManager.migrateLegacyHidden((ServerPlayer) (Object) this, input);
    }
}
