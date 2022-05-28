package xyz.jpenilla.squaremap.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.jpenilla.squaremap.fabric.event.ServerPlayerEvents;

@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    private final ThreadLocal<ServerLevel> preRespawnLevel = new ThreadLocal<>();

    @Inject(
        method = "respawn(Lnet/minecraft/server/level/ServerPlayer;Z)Lnet/minecraft/server/level/ServerPlayer;",
        at = @At("HEAD")
    )
    void injectRespawnHead(ServerPlayer serverPlayer, boolean bl, CallbackInfoReturnable<ServerPlayer> cir) {
        this.preRespawnLevel.set(serverPlayer.getLevel());
    }

    @Inject(
        method = "respawn(Lnet/minecraft/server/level/ServerPlayer;Z)Lnet/minecraft/server/level/ServerPlayer;",
        at = @At("RETURN")
    )
    void injectRespawnReturn(ServerPlayer oldPlayer, boolean bl, CallbackInfoReturnable<ServerPlayer> cir) {
        final ServerLevel oldLevel = this.preRespawnLevel.get();
        this.preRespawnLevel.remove();
        final ServerPlayer player = cir.getReturnValue();
        if (player.getLevel() == oldLevel) {
            return;
        }
        ServerPlayerEvents.WORLD_CHANGED.invoker().worldChanged(player);
    }
}
