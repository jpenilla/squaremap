package xyz.jpenilla.squaremap.sponge.listener;

import com.google.inject.Inject;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import xyz.jpenilla.squaremap.sponge.SpongeWorldManager;

@DefaultQualifier(NonNull.class)
public final class WorldLoadListener {
    private final SpongeWorldManager worldManager;

    @Inject
    private WorldLoadListener(final SpongeWorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Listener(order = Order.EARLY)
    public void worldLoad(final LoadWorldEvent event) {
        this.worldManager.initWorld((ServerLevel) event.world());
    }

    @Listener(order = Order.LATE)
    public void worldUnload(final UnloadWorldEvent event) {
        this.worldManager.worldUnloaded((ServerLevel) event.world());
    }
}
