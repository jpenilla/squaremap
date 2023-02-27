package xyz.jpenilla.squaremap.fabric.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkMapAccess extends xyz.jpenilla.squaremap.common.util.ChunkMapAccess {
    @Invoker("getVisibleChunkIfPresent")
    @Override
    ChunkHolder squaremap$getVisibleChunkIfPresent(long pos);

    @Invoker("readChunk")
    @Override
    CompletableFuture<Optional<CompoundTag>> squaremap$readChunk(ChunkPos pos);

    @Accessor("pendingUnloads")
    @Override
    Long2ObjectLinkedOpenHashMap<ChunkHolder> squaremap$pendingUnloads();
}
