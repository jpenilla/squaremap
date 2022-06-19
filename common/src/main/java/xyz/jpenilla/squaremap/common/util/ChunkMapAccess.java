package xyz.jpenilla.squaremap.common.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;

public interface ChunkMapAccess {
    ChunkHolder squaremap$getVisibleChunkIfPresent(long pos);

    CompletableFuture<Optional<CompoundTag>> squaremap$readChunk(ChunkPos pos);

    Long2ObjectLinkedOpenHashMap<ChunkHolder> squaremap$pendingUnloads();
}
