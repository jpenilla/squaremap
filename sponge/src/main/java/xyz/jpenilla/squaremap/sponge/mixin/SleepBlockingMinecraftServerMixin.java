package xyz.jpenilla.squaremap.sponge.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.jpenilla.squaremap.common.util.SleepBlockingMinecraftServer;

@Mixin(MinecraftServer.class)
abstract class SleepBlockingMinecraftServerMixin implements SleepBlockingMinecraftServer {
    @Unique
    private volatile boolean allowSleep = true;

    // Use redirect instead of WrapOperation as Sponge doesn't have MixinExtras
    @Redirect(
        method = "tickServer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;getPlayerCount()I"
        )
    )
    private int allowSleep(final PlayerList instance) {
        return this.allowSleep ? instance.getPlayerCount() : 1;
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
