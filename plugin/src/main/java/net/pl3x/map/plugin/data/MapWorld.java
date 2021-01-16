package net.pl3x.map.plugin.data;

import net.minecraft.server.v1_16_R3.World;
import net.pl3x.map.plugin.configuration.WorldConfig;
import net.pl3x.map.plugin.task.render.AbstractRender;
import net.pl3x.map.plugin.task.render.BackgroundRender;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class MapWorld {
    private final World world;
    private final org.bukkit.World bukkitWorld;
    private final ExecutorService imageIOexecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Set<ChunkCoordinate> modifiedChunks = ConcurrentHashMap.newKeySet();

    private AbstractRender activeRender = null;
    private ScheduledFuture<?> backgroundRender = null;

    private MapWorld(final org.bukkit.@NonNull World world) {
        this.bukkitWorld = world;
        this.world = ((CraftWorld) world).getHandle();
        this.startBackgroundRender();
    }

    private void startBackgroundRender() {
        if (this.backgroundRendering() || this.isRendering()) {
            throw new IllegalStateException("Already rendering");
        }
        if (!this.config().BACKGROUND_RENDER_ENABLED) {
            return;
        }
        final BackgroundRender render = new BackgroundRender(this);
        this.backgroundRender = this.executor.scheduleAtFixedRate(render, this.config().BACKGROUND_RENDER_INTERVAL_SECONDS, this.config().BACKGROUND_RENDER_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void stopBackgroundRender() {
        if (!this.backgroundRendering()) {
            throw new IllegalStateException("Not background rendering");
        }
        this.backgroundRender.cancel(false);
        this.backgroundRender = null;
    }

    private boolean backgroundRendering() {
        return this.backgroundRender != null;
    }

    public void chunkModified(final @NonNull ChunkCoordinate coord) {
        this.modifiedChunks.add(coord);
    }

    public boolean hasModifiedChunks() {
        return !this.modifiedChunks.isEmpty();
    }

    public @NonNull ChunkCoordinate nextModifiedChunk() {
        final Iterator<ChunkCoordinate> it = this.modifiedChunks.iterator();
        final ChunkCoordinate coord = it.next();
        it.remove();
        return coord;
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
        this.startBackgroundRender();
    }

    public void startRender(final @NonNull AbstractRender render) {
        if (this.isRendering()) {
            throw new IllegalStateException("Already rendering");
        }
        if (this.backgroundRendering()) {
            this.stopBackgroundRender();
        }
        this.activeRender = render;
        executor.submit(this.activeRender.getFutureTask());
    }

    public void shutdown() {
        if (this.isRendering()) {
            this.stopRender();
        }
        if (this.backgroundRendering()) {
            this.stopBackgroundRender();
        }
        executor.shutdown();
        imageIOexecutor.shutdown();
    }

    public void saveImage(final @NonNull Image image) {
        this.imageIOexecutor.submit(image::save);
    }
}
