package xyz.jpenilla.squaremap.sponge.listener;

import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import xyz.jpenilla.squaremap.common.config.WorldConfig;
import xyz.jpenilla.squaremap.sponge.SquaremapSponge;

@DefaultQualifier(NonNull.class)
public record WorldLoadListener(SquaremapSponge platform) {
    @Listener(order = Order.EARLY)
    public void worldLoad(final LoadWorldEvent event) {
        WorldConfig.get((ServerLevel) event.world());
        this.platform.worldManager().getWorldIfEnabled((ServerLevel) event.world());
    }

    @Listener(order = Order.LATE)
    public void worldUnload(final UnloadWorldEvent event) {
        this.platform.worldManager().worldUnloaded((ServerLevel) event.world());
    }
}
