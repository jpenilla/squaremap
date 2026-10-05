package xyz.jpenilla.squaremap.common.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.coordinate.RegionCoordinate;
import xyz.jpenilla.squaremap.common.util.FileUtil;
import xyz.jpenilla.squaremap.common.util.Json;

/**
 * Persists foreground render progress so it can be resumed after a restart.
 */
record RenderCheckpointStore(Path file) {
    private static final int VERSION = 1;

    record Checkpoint(RenderJob.Mode mode, RenderPlan plan) {
    }

    private record Data(int version, RenderJob.@Nullable Mode mode, @Nullable List<RenderPlan.SavedRegion> regions) {
    }

    /**
     * Reads saved progress. Progress that cannot be read is logged and deleted.
     *
     * @return the checkpoint, or {@code null} if none was saved or it could not be read
     */
    @Nullable Checkpoint read() {
        if (!Files.isRegularFile(this.file)) {
            return null;
        }
        try (final BufferedReader reader = Files.newBufferedReader(this.file)) {
            final JsonElement json = JsonParser.parseReader(reader);
            if (json.isJsonArray()) {
                return readLegacy(json.getAsJsonArray());
            }
            final Data data = Json.gson().fromJson(json, Data.class);
            if (data == null || data.version() != VERSION || data.mode() == null || data.mode() == RenderJob.Mode.BACKGROUND || data.regions() == null) {
                throw new IllegalArgumentException("Unsupported render progress format");
            }
            return new Checkpoint(data.mode(), RenderPlan.restore(data.regions()));
        } catch (final IOException | RuntimeException ex) {
            Logging.logger().warn("Discarding unreadable render progress from '{}'", this.file, ex);
            this.delete();
            return null;
        }
    }

    /**
     * Converts full render progress saved by older versions, which recorded whether each region was done.
     */
    private static Checkpoint readLegacy(final JsonArray entries) {
        final long[] allChunks = new long[16];
        Arrays.fill(allChunks, -1L);
        final List<RenderPlan.SavedRegion> regions = new ArrayList<>();
        for (final JsonElement entry : entries) {
            final JsonArray pair = entry.getAsJsonArray();
            final RegionCoordinate region = Json.gson().fromJson(pair.get(0), RegionCoordinate.class);
            regions.add(new RenderPlan.SavedRegion(region, allChunks, new long[0], pair.get(1).getAsBoolean()));
        }
        return new Checkpoint(RenderJob.Mode.FULL, RenderPlan.restore(regions));
    }

    void save(final RenderJob.Mode mode, final RenderPlan plan) {
        try {
            FileUtil.atomicWrite(this.file, tmp -> Files.writeString(tmp, Json.gson().toJson(new Data(VERSION, mode, plan.save()))));
        } catch (final IOException ex) {
            Logging.logger().warn("Failed to save render progress to '{}'", this.file, ex);
        }
    }

    void delete() {
        try {
            Files.deleteIfExists(this.file);
        } catch (final IOException ex) {
            Logging.logger().warn("Failed to delete render progress from '{}'", this.file, ex);
        }
    }
}
