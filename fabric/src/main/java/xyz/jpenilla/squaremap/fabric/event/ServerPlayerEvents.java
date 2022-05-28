package xyz.jpenilla.squaremap.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

public final class ServerPlayerEvents {
    public static final Event<WorldChanged> WORLD_CHANGED = EventFactory.createArrayBacked(
        WorldChanged.class,
        listeners -> player -> {
            for (final WorldChanged listener : listeners) {
                listener.worldChanged(player);
            }
        }
    );

    private ServerPlayerEvents() {
    }

    public interface WorldChanged {
        void worldChanged(ServerPlayer player);
    }
}
