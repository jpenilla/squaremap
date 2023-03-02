package xyz.jpenilla.squaremap.forge.mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.jpenilla.squaremap.forge.event.ChunkGenerateEvent;

@Mixin(ChunkHolder.class)
abstract class ChunkHolderMixin {
    @Inject(
        method = "replaceProtoChunk(Lnet/minecraft/world/level/chunk/ImposterProtoChunk;)V",
        at = @At("TAIL")
    )
    void chunkGenerated(ImposterProtoChunk imposter, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new ChunkGenerateEvent(
            (ServerLevel) imposter.getWrapped().getLevel(),
            imposter.getPos()
        ));
    }
}
