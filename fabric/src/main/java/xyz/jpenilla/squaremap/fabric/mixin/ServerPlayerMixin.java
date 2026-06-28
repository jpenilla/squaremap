package xyz.jpenilla.squaremap.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.jpenilla.squaremap.fabric.FabricPlayerManager;
import xyz.jpenilla.squaremap.fabric.event.ServerPlayerEvents;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @Inject(
        method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;sendLevelInfo(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER
        )
    )
    void injectChangeDimension(final CallbackInfoReturnable<Entity> cir) {
        ServerPlayerEvents.WORLD_CHANGED.invoker().worldChanged((ServerPlayer) (Object) this);
    }

    @Inject(
        method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V",
        at = @At("HEAD")
    )
    private void squaremap$migrateLegacyHidden(final ValueInput input, final CallbackInfo ci) {
        FabricPlayerManager.migrateLegacyHidden((ServerPlayer) (Object) this, input);
    }
}
