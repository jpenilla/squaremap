package xyz.jpenilla.squaremap.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class MapUpdateEvents {
    public static final Event<PositionListener<BlockPos>> BLOCK_CHANGED = EventFactory.createArrayBacked(
        PositionListener.class,
        listeners -> (serverLevel, position) -> {
            for (final PositionListener<BlockPos> listener : listeners) {
                listener.updatePosition(serverLevel, position);
            }
        }
    );

    public static final Event<PositionListener<ChunkPos>> CHUNK_CHANGED = EventFactory.createArrayBacked(
        PositionListener.class,
        listeners -> (serverLevel, position) -> {
            for (final PositionListener<ChunkPos> listener : listeners) {
                listener.updatePosition(serverLevel, position);
            }
        }
    );

    private MapUpdateEvents() {
    }

    public interface PositionListener<T> {
        void updatePosition(ServerLevel serverLevel, T position);
    }
}
