package xyz.jpenilla.squaremap.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.jpenilla.squaremap.fabric.event.ServerPlayerEvents;

@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    private final ThreadLocal<ServerLevel> preRespawnLevel = new ThreadLocal<>();

    @Inject(
        method = "Lnet/minecraft/server/players/PlayerList;respawn(Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/world/entity/Entity$RemovalReason;)Lnet/minecraft/server/level/ServerPlayer;",
        at = @At("HEAD")
    )
    void injectRespawnHead(ServerPlayer serverPlayer, boolean bl, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        this.preRespawnLevel.set((ServerLevel) serverPlayer.level());
    }

    @Inject(
        method = "Lnet/minecraft/server/players/PlayerList;respawn(Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/world/entity/Entity$RemovalReason;)Lnet/minecraft/server/level/ServerPlayer;",
        at = @At("RETURN")
    )
    void injectRespawnReturn(ServerPlayer serverPlayer, boolean bl, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        final ServerLevel oldLevel = this.preRespawnLevel.get();
        this.preRespawnLevel.remove();
        final ServerPlayer player = cir.getReturnValue();
        if (player.level() == oldLevel) {
            return;
        }
        ServerPlayerEvents.WORLD_CHANGED.invoker().worldChanged(player);
    }
}
