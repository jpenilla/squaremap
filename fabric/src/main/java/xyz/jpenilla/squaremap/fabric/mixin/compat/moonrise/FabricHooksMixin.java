package xyz.jpenilla.squaremap.fabric.mixin.compat.moonrise;

import ca.spottedleaf.moonrise.fabric.FabricHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.jpenilla.squaremap.fabric.event.MapUpdateEvents;

@Mixin(FabricHooks.class)
abstract class FabricHooksMixin {
    @Inject(
        method = "chunkFullStatusComplete",
        at = @At("TAIL")
    )
    private void chunkFullStatusComplete(final LevelChunk newChunk, final ProtoChunk original, final CallbackInfo ci) {
        if (!(original instanceof ImposterProtoChunk)) {
            MapUpdateEvents.CHUNK_CHANGED.invoker().updatePosition(
                (ServerLevel) newChunk.getLevel(),
                newChunk.getPos()
            );
        }
    }
}
