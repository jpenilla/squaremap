package xyz.jpenilla.squaremap.sponge.mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.sponge.config.SpongeAdvanced;

// https://github.com/SpongePowered/Sponge/issues/3582
@Mixin(ChunkHolder.class)
abstract class ChunkHolderMixin {
    @Inject(
        method = "replaceProtoChunk(Lnet/minecraft/world/level/chunk/ImposterProtoChunk;)V",
        at = @At("TAIL")
    )
    void chunkGenerated(ImposterProtoChunk imposter, CallbackInfo ci) {
        if (!SpongeAdvanced.CHUNK_GENERATION) {
            return;
        }

        final ServerLevel level = (ServerLevel) imposter.getWrapped().getLevel();
        SquaremapCommon.instance().platform().worldManager().getWorldIfEnabled(level).ifPresent(world -> {
            final ChunkPos pos = imposter.getPos();
            world.chunkModified(new ChunkCoordinate(pos.x, pos.z));
        });
    }
}
