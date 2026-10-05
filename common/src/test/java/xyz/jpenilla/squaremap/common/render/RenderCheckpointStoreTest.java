package xyz.jpenilla.squaremap.common.render;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.common.coordinate.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.coordinate.RegionCoordinate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderCheckpointStoreTest {
    @TempDir
    Path directory;

    private RenderCheckpointStore store() {
        return new RenderCheckpointStore(this.directory.resolve("resume_render.json"));
    }

    @Test
    void roundTripsModeAndProgress() throws Exception {
        final RenderPlan plan = RenderPlan.chunks(List.of(new ChunkCoordinate(0, 31)), chunk -> true);
        plan.regions().getLast().complete();
        this.store().save(RenderJob.Mode.RADIUS, plan);

        final RenderCheckpointStore.Checkpoint checkpoint = this.store().read();
        assertNotNull(checkpoint);
        assertEquals(RenderJob.Mode.RADIUS, checkpoint.mode());
        assertFalse(checkpoint.plan().regions().getFirst().completed());
        assertTrue(checkpoint.plan().regions().getLast().completed());
        assertEquals(plan.regions().getLast().chunks(), checkpoint.plan().regions().getLast().chunks());
    }

    @Test
    void convertsLegacyFullRenderProgress() throws Exception {
        Files.writeString(this.store().file(), "[[{\"x\":0,\"z\":0},true],[{\"x\":-1,\"z\":2},false]]");

        final RenderCheckpointStore.Checkpoint checkpoint = this.store().read();
        assertNotNull(checkpoint);
        assertEquals(RenderJob.Mode.FULL, checkpoint.mode());
        assertEquals(List.of(new RegionCoordinate(0, 0), new RegionCoordinate(-1, 2)),
            checkpoint.plan().regions().stream().map(RenderPlan.RegionWork::coordinate).toList());
        assertTrue(checkpoint.plan().regions().getFirst().completed());
        assertFalse(checkpoint.plan().regions().getLast().completed());
        assertEquals(1024, checkpoint.plan().regions().getLast().chunkCount());
    }

    @Test
    void discardsUnreadableProgress() throws Exception {
        for (final String content : List.of("{", "{\"version\":2,\"mode\":\"FULL\",\"regions\":[]}")) {
            Files.writeString(this.store().file(), content);
            assertNull(this.store().read());
            assertFalse(Files.exists(this.store().file()));
        }
    }
}
