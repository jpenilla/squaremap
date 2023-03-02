package xyz.jpenilla.squaremap.forge.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.eventbus.api.Event;

public class ChunkGenerateEvent extends Event {
    private final ServerLevel level;
    private final ChunkPos pos;

    public ChunkGenerateEvent(final ServerLevel level, final ChunkPos pos) {
        this.level = level;
        this.pos = pos;
    }

    public ServerLevel level() {
        return this.level;
    }

    public ChunkPos chunkPos() {
        return this.pos;
    }
}
