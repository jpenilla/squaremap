package net.pl3x.map.data;

import net.minecraft.server.v1_16_R3.World;
import net.pl3x.map.configuration.WorldConfig;
import net.pl3x.map.task.AbstractRender;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MapWorld {
    private final World world;
    private final org.bukkit.World bukkitWorld;
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    private AbstractRender activeRender = null;

    private MapWorld(final org.bukkit.@NonNull World world) {
        this.bukkitWorld = world;
        this.world = ((CraftWorld) world).getHandle();
    }

    public static @NonNull MapWorld forWorld(final org.bukkit.@NonNull World world) {
        return new MapWorld(world);
    }

    public @NonNull String name() {
        return this.bukkitWorld.getName();
    }

    public @NonNull WorldConfig config() {
        return WorldConfig.get(this.bukkitWorld);
    }

    public org.bukkit.@NonNull World bukkit() {
        return this.bukkitWorld;
    }

    public boolean isRendering() {
        return this.activeRender != null;
    }

    public void stopRender() {
        if (!this.isRendering()) {
            throw new IllegalStateException("No render to stop");
        }
        this.activeRender.cancel();
        this.activeRender = null;
    }

    public void startRender(final @NonNull AbstractRender render) {
        if (this.isRendering()) {
            throw new IllegalStateException("Already rendering");
        }
        this.activeRender = render;
        service.submit(this.activeRender.getFutureTask());
    }

    public void shutdown() {
        if (this.isRendering()) {
            this.stopRender();
        }
        service.shutdown();
    }
}
