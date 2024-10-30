package xyz.jpenilla.squaremap.forge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xyz.jpenilla.squaremap.common.util.SleepBlockingMinecraftServer;

@Mixin(MinecraftServer.class)
abstract class SleepBlockingMinecraftServerMixin implements SleepBlockingMinecraftServer {
    @Unique
    private volatile boolean allowSleep = true;

    @WrapOperation(
        method = "tickServer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;getPlayerCount()I"
        )
    )
    private int allowSleep(final PlayerList instance, final Operation<Integer> original) {
        return this.allowSleep ? original.call(instance) : 1;
    }

    @Override
    public void squaremap$blockSleep() {
        this.allowSleep = false;
    }

    @Override
    public void squaremap$allowSleep() {
        this.allowSleep = true;
    }
}
