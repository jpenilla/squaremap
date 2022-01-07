package xyz.jpenilla.squaremap.fabric.mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.jpenilla.squaremap.fabric.util.FabricMapUpdates;

@Mixin(ChunkHolder.class)
abstract class ChunkHolderMixin {
    @Inject(
        method = "replaceProtoChunk(Lnet/minecraft/world/level/chunk/ImposterProtoChunk;)V",
        at = @At("TAIL")
    )
    void chunkGenerated(ImposterProtoChunk imposter, CallbackInfo ci) {
        FabricMapUpdates.mark((ServerLevel) imposter.getWrapped().getLevel(), imposter.getPos());
    }
}
