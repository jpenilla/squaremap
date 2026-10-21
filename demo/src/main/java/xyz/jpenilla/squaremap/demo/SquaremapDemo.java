package xyz.jpenilla.squaremap.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.popcraft.chunky.api.ChunkyAPI;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;

/**
 * Pre-generates or renders every loaded world out to a fixed radius, depending on {@code squaremap.demo.mode},
 * writes {@code demo-result.json} to the server directory, then shuts the server down.
 */
@DefaultQualifier(NonNull.class)
public final class SquaremapDemo extends JavaPlugin {
    private final Deque<World> remaining = new ArrayDeque<>();
    private @Nullable ChunkyAPI chunky;
    private Mode mode;
    private int radius;

    @Override
    public void onEnable() {
        this.mode = Mode.valueOf(System.getProperty("squaremap.demo.mode").toUpperCase(Locale.ROOT));
        this.radius = Integer.parseInt(System.getProperty("squaremap.demo.radius"));
        if (this.mode == Mode.PREGEN) {
            this.chunky = this.getServer().getServicesManager().load(ChunkyAPI.class);
            this.chunky.onGenerationProgress(event -> {
                if (event.complete()) {
                    this.getServer().getScheduler().runTask(this, this::next);
                }
            });
        }
        // Runs on the first tick, after all worlds have loaded
        this.getServer().getScheduler().runTask(this, () -> {
            this.remaining.addAll(this.getServer().getWorlds());
            this.next();
        });
    }

    private void next() {
        final @Nullable World world = this.remaining.poll();
        if (world == null) {
            this.finish(null);
            return;
        }
        switch (this.mode) {
            case PREGEN -> this.pregen(world);
            case RENDER -> this.render(world);
        }
    }

    private void pregen(final World world) {
        this.getLogger().info("Generating " + world.getName() + " with radius " + this.radius);
        if (!this.chunky.startTask(world.getName(), "square", 0, 0, this.radius, this.radius, "loop")) {
            this.finish("Failed to start Chunky task for " + world.getName());
        }
    }

    private void render(final World world) {
        final MapWorldInternal mapWorld = (MapWorldInternal) SquaremapProvider.get()
            .getWorldIfEnabled(BukkitAdapter.worldIdentifier(world))
            .orElseThrow();
        if (!mapWorld.renderScheduler().startRadiusRender(BlockPos.ZERO, this.radius)) {
            this.finish("Failed to start render for " + world.getName());
            return;
        }
        this.getServer().getScheduler().runTaskTimer(this, (final BukkitTask task) -> {
            if (!mapWorld.renderScheduler().isRendering()) {
                task.cancel();
                this.next();
            }
        }, 20L, 20L);
    }

    private void finish(final @Nullable String error) {
        final String result = error == null
            ? "{\"status\":\"ok\"}"
            : "{\"status\":\"error\",\"message\":\"" + error.replace("\"", "\\\"") + "\"}";
        try {
            Files.writeString(Path.of("demo-result.json"), result);
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
        this.getServer().shutdown();
    }

    private enum Mode {
        PREGEN,
        RENDER
    }
}
