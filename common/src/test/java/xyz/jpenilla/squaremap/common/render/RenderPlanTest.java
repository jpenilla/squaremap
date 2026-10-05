package xyz.jpenilla.squaremap.common.render;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import xyz.jpenilla.squaremap.common.coordinate.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.coordinate.RegionCoordinate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderPlanTest {
    @Test
    void fullRegionsUseBoundedColumnRuns() throws Exception {
        final RenderPlan plan = RenderPlan.regions(List.of(new RegionCoordinate(-1, -1)), chunk -> chunk.z() < 0);
        final RenderPlan.RegionWork region = plan.regions().getFirst();
        final var columns = region.columns(3);
        assertTrue(columns.stream().allMatch(column -> !column.isEmpty() && column.size() <= 3));
        assertEquals(1024, columns.stream().mapToInt(List::size).sum());
    }

    @Test
    void sparseColumnsSplitAtGapsAndKeepSouthernCorrections() throws Exception {
        final List<ChunkCoordinate> selected = List.of(
            new ChunkCoordinate(0, 0), new ChunkCoordinate(0, 1),
            new ChunkCoordinate(0, 5), new ChunkCoordinate(1, 0)
        );
        final RenderPlan plan = RenderPlan.chunks(selected, chunk -> true);
        final var chunks = plan.regions().getFirst().columns(128).stream().flatMap(List::stream).toList();
        assertEquals(7, chunks.size());
        assertEquals(Set.copyOf(selected), chunks.stream().filter(chunk -> !chunk.topRowOnly())
            .map(RenderPlan.ChunkWork::coordinate).collect(Collectors.toSet()));
        assertEquals(Set.of(new ChunkCoordinate(0, 2), new ChunkCoordinate(0, 6), new ChunkCoordinate(1, 1)),
            chunks.stream().filter(RenderPlan.ChunkWork::topRowOnly).map(RenderPlan.ChunkWork::coordinate).collect(Collectors.toSet()));
    }

    @Test
    void southernCorrectionsHaveUniqueOwnershipAcrossRegionBoundaries() throws Exception {
        for (final ChunkCoordinate source : List.of(new ChunkCoordinate(0, 31), new ChunkCoordinate(-1, -1))) {
            final ChunkCoordinate south = new ChunkCoordinate(source.x(), source.z() + 1);
            final RenderPlan plan = RenderPlan.chunks(List.of(source), chunk -> true);
            assertEquals(2, plan.regions().size());
            assertEquals(Set.of(source.regionCoordinate(), south.regionCoordinate()),
                plan.regions().stream().map(RenderPlan.RegionWork::coordinate).collect(Collectors.toSet()));
            final var chunks = plan.regions().stream().flatMap(region -> region.chunks().stream()).toList();
            assertEquals(2, chunks.size());
            assertEquals(Set.of(new RenderPlan.ChunkWork(source, false), new RenderPlan.ChunkWork(south, true)), Set.copyOf(chunks));
            assertEquals(1, plan.regions().stream().mapToInt(RenderPlan.RegionWork::chunkCount).sum());
            for (final var region : plan.regions()) {
                assertTrue(region.chunks().stream().allMatch(chunk -> chunk.coordinate().regionCoordinate().equals(region.coordinate())));
            }
        }
    }

    @Test
    void fullChunkSupersedesCorrectionAndDuplicates() throws Exception {
        final RenderPlan plan = RenderPlan.chunks(List.of(
            new ChunkCoordinate(0, 31), new ChunkCoordinate(0, 32), new ChunkCoordinate(0, 32)
        ), chunk -> true);
        final RenderPlan.RegionWork south = plan.regions().getLast();
        assertEquals(Set.of(
            new RenderPlan.ChunkWork(new ChunkCoordinate(0, 32), false),
            new RenderPlan.ChunkWork(new ChunkCoordinate(0, 33), true)
        ), Set.copyOf(south.chunks()));
        assertEquals(2, south.chunks().size());
    }

    @Test
    void selectionAndCorrectionsRespectVisibility() throws Exception {
        assertTrue(RenderPlan.chunks(List.<ChunkCoordinate>of(), chunk -> true).regions().isEmpty());
        assertTrue(RenderPlan.chunks(List.of(new ChunkCoordinate(0, 0)), chunk -> false).regions().isEmpty());
        final RenderPlan plan = RenderPlan.chunks(List.of(new ChunkCoordinate(0, 31)), chunk -> chunk.z() <= 31);
        assertEquals(1, plan.regions().size());
        assertEquals(1, plan.regions().getFirst().chunks().size());
    }

    @Test
    void fullRegionPlanningStaysWithinListedRegions() throws Exception {
        final RenderPlan plan = RenderPlan.regions(List.of(new RegionCoordinate(0, 0), new RegionCoordinate(0, 1)), chunk -> true);
        assertEquals(List.of(new RegionCoordinate(0, 0), new RegionCoordinate(0, 1)),
            plan.regions().stream().map(RenderPlan.RegionWork::coordinate).toList());
        assertTrue(plan.regions().stream().allMatch(region -> region.chunks().size() == 1024 && region.chunks().stream().noneMatch(RenderPlan.ChunkWork::topRowOnly)));
    }

    @Test
    void retainOnlyRemovesInvisibleWork() throws Exception {
        final RenderPlan plan = RenderPlan.chunks(List.of(new ChunkCoordinate(0, 30), new ChunkCoordinate(1, 30)), chunk -> true);
        plan.retain(chunk -> chunk.x() == 0);
        final var chunks = plan.regions().stream().flatMap(region -> region.chunks().stream()).toList();
        assertEquals(Set.of(
            new RenderPlan.ChunkWork(new ChunkCoordinate(0, 30), false),
            new RenderPlan.ChunkWork(new ChunkCoordinate(0, 31), true)
        ), Set.copyOf(chunks));
    }
}
