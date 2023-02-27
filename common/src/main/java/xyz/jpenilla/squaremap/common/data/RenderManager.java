package xyz.jpenilla.squaremap.common.data;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.Pair;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.task.render.AbstractRender;
import xyz.jpenilla.squaremap.common.task.render.BackgroundRender;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;
import xyz.jpenilla.squaremap.common.util.ExceptionLoggingScheduledThreadPoolExecutor;
import xyz.jpenilla.squaremap.common.util.Util;

@DefaultQualifier(NonNull.class)
public final class RenderManager {
    private static final Gson GSON = Util.gson()
        .newBuilder()
        .enableComplexMapKeySerialization()
        .create();
    private static final String RENDER_PROGRESS_FILE_NAME = "resume_render.json";

    private final MapWorldInternal mapWorld;
    private final ScheduledExecutorService executor;
    private final RenderFactory renderFactory;

    private volatile boolean pauseRenders = false;
    private volatile @Nullable Pair<AbstractRender, Future<?>> activeRender = null;
    private volatile @Nullable Pair<BackgroundRender, Future<?>> backgroundRender = null;

    private RenderManager(final MapWorldInternal mapWorld, final RenderFactory renderFactory) {
        this.mapWorld = mapWorld;
        this.renderFactory = renderFactory;
        this.executor = new ExceptionLoggingScheduledThreadPoolExecutor(
            1,
            Util.squaremapThreadFactory("render", mapWorld.serverLevel())
        );
    }

    public void init() {
        this.startBackgroundRender();

        if (this.readRenderProgress() != null) {
            this.startRender(this.renderFactory.createFullRender(this.mapWorld, 2));
        }
    }

    public boolean isRendering() {
        return this.activeRender != null;
    }

    public boolean rendersPaused() {
        return this.pauseRenders;
    }

    public void pauseRenders(final boolean pauseRenders) {
        this.pauseRenders = pauseRenders;
    }

    public void restartRenderProgressLogging() {
        final @Nullable Pair<AbstractRender, Future<?>> render = this.activeRender;
        if (render != null) {
            render.left().restartProgressLogger();
        }
    }

    public void startRender(final AbstractRender render) {
        if (this.isRendering()) {
            throw new IllegalStateException("Already rendering");
        }
        if (this.backgroundRendering()) {
            this.stopBackgroundRender();
        }
        this.activeRender = Pair.of(
            render,
            this.executor.submit(render)
        );
    }

    public void cancelRender() {
        final @Nullable Pair<AbstractRender, Future<?>> render = this.activeRender;
        if (render == null) {
            throw new IllegalStateException("No render to cancel");
        }
        render.left().cancel();
        waitFor(render.right());
    }

    private void stopRender() {
        final @Nullable Pair<AbstractRender, Future<?>> render = this.activeRender;
        if (render == null) {
            throw new IllegalStateException("No render to stop");
        }
        render.left().stop();
        waitFor(render.right());
    }

    public void renderStopped(final boolean deleteProgress) {
        this.activeRender = null;
        if (deleteProgress) {
            try {
                Files.deleteIfExists(this.mapWorld.dataPath().resolve(RENDER_PROGRESS_FILE_NAME));
            } catch (final IOException ex) {
                Logging.logger().warn("Failed to delete render state data for world '{}'", this.mapWorld.identifier().asString(), ex);
            }
        }
        this.startBackgroundRender();
    }

    private void startBackgroundRender() {
        if (this.backgroundRendering() || this.isRendering()) {
            throw new IllegalStateException("Already rendering");
        }
        if (!this.mapWorld.config().BACKGROUND_RENDER_ENABLED) {
            return;
        }
        final BackgroundRender render = this.renderFactory.createBackgroundRender(this.mapWorld);
        this.backgroundRender = Pair.of(
            render,
            this.executor.scheduleAtFixedRate(
                render,
                this.mapWorld.config().BACKGROUND_RENDER_INTERVAL_SECONDS,
                this.mapWorld.config().BACKGROUND_RENDER_INTERVAL_SECONDS,
                TimeUnit.SECONDS
            )
        );
    }

    private void stopBackgroundRender() {
        final @Nullable Pair<BackgroundRender, Future<?>> pair = this.backgroundRender;
        if (pair == null) {
            throw new IllegalStateException("Not background rendering");
        }
        pair.right().cancel(false);
        pair.left().stop();
        this.backgroundRender = null;
    }

    private boolean backgroundRendering() {
        return this.backgroundRender != null;
    }

    public @Nullable Map<RegionCoordinate, Boolean> readRenderProgress() {
        final Path file = this.mapWorld.dataPath().resolve(RENDER_PROGRESS_FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (final BufferedReader reader = Files.newBufferedReader(file)) {
            return GSON.fromJson(reader, new TypeToken<LinkedHashMap<RegionCoordinate, Boolean>>() {}.getType());
        } catch (final JsonIOException | JsonSyntaxException | IOException e) {
            Logging.logger().warn("Failed to deserialize render progress for world '{}' from file '{}'", this.mapWorld.identifier().asString(), file, e);
        }
        return null;
    }

    public void saveRenderProgress(final Map<RegionCoordinate, Boolean> regions) {
        final Path file = this.mapWorld.dataPath().resolve(RENDER_PROGRESS_FILE_NAME);
        try {
            Files.writeString(file, GSON.toJson(regions));
        } catch (final IOException ex) {
            Logging.logger().warn("Failed to serialize render progress for world '{}' to file '{}'", this.mapWorld.identifier().asString(), file, ex);
        }
    }

    public void shutdown() {
        if (this.isRendering()) {
            this.stopRender();
        }
        if (this.backgroundRendering()) {
            this.stopBackgroundRender();
        }
        Util.shutdownExecutor(this.executor, TimeUnit.SECONDS, 1L);
    }

    private static void waitFor(final Future<?> future) {
        try {
            future.get();
        } catch (final InterruptedException | ExecutionException | CancellationException ignore) {
        }
    }

    public static RenderManager create(final MapWorldInternal mapWorld, final RenderFactory renderFactory) {
        return new RenderManager(mapWorld, renderFactory);
    }
}
