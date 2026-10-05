package xyz.jpenilla.squaremap.common.render;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.coordinate.ChunkCoordinate;
import xyz.jpenilla.squaremap.common.coordinate.RegionCoordinate;

import static xyz.jpenilla.squaremap.common.util.concurrent.Threads.throwIfInterrupted;

/**
 * Records full-chunk and north-edge-row work and its completion state, grouped by destination region.
 */
@DefaultQualifier(NonNull.class)
public final class RenderPlan {
    private final List<RegionWork> regions;

    private RenderPlan(final List<RegionWork> regions) {
        this.regions = List.copyOf(regions);
    }

    static RenderPlan chunks(final Iterable<ChunkCoordinate> chunks, final Predicate<ChunkCoordinate> visible) throws InterruptedException {
        final Map<RegionCoordinate, RegionWork> regions = new LinkedHashMap<>();
        for (final ChunkCoordinate chunk : chunks) {
            throwIfInterrupted();
            if (visible.test(chunk)) {
                region(regions, chunk.regionCoordinate()).renderChunks.set(index(chunk));
            }
        }
        return finish(regions, visible);
    }

    static RenderPlan regions(final Iterable<RegionCoordinate> coordinates, final Predicate<ChunkCoordinate> visible) throws InterruptedException {
        final Map<RegionCoordinate, RegionWork> regions = new LinkedHashMap<>();
        for (final RegionCoordinate coordinate : coordinates) {
            throwIfInterrupted();
            final RegionWork work = region(regions, coordinate);
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    final ChunkCoordinate chunk = new ChunkCoordinate(coordinate.getChunkX() + x, coordinate.getChunkZ() + z);
                    if (visible.test(chunk)) {
                        work.renderChunks.set(index(chunk));
                    }
                }
            }
        }
        // Every visible chunk of each region renders in full, so no north-row corrections are needed.
        // Regions outside the list are not added: without a region file they have no chunks to correct.
        return new RenderPlan(new ArrayList<>(regions.values()));
    }

    private static RenderPlan finish(final Map<RegionCoordinate, RegionWork> regions, final Predicate<ChunkCoordinate> visible) throws InterruptedException {
        // Iterate a copy: southern corrections may introduce new destination regions.
        for (final RegionWork work : List.copyOf(regions.values())) {
            throwIfInterrupted();
            for (int bit = work.renderChunks.nextSetBit(0); bit >= 0; bit = work.renderChunks.nextSetBit(bit + 1)) {
                final ChunkCoordinate chunk = work.chunk(bit);
                // Refresh the southern neighbor's north-edge row: its shading depends on this chunk's heights.
                final ChunkCoordinate south = new ChunkCoordinate(chunk.x(), chunk.z() + 1);
                if (visible.test(south)) {
                    region(regions, south.regionCoordinate()).renderNorthRowChunks.set(index(south));
                }
            }
        }
        regions.values().forEach(work -> work.renderNorthRowChunks.andNot(work.renderChunks));
        return new RenderPlan(new ArrayList<>(regions.values()));
    }

    private static RegionWork region(final Map<RegionCoordinate, RegionWork> regions, final RegionCoordinate coordinate) {
        return regions.computeIfAbsent(coordinate, RegionWork::new);
    }

    private static int index(final ChunkCoordinate chunk) {
        return (chunk.x() & 31) * 32 + (chunk.z() & 31);
    }

    List<RegionWork> regions() {
        return this.regions;
    }

    List<SavedRegion> save() {
        return this.regions.stream().map(work -> new SavedRegion(
            work.coordinate, work.renderChunks.toLongArray(), work.renderNorthRowChunks.toLongArray(), work.completed
        )).toList();
    }

    static RenderPlan restore(final List<SavedRegion> saved) {
        final List<RegionWork> regions = new ArrayList<>();
        for (final SavedRegion region : saved) {
            final RegionWork work = new RegionWork(region.coordinate);
            work.renderChunks.or(BitSet.valueOf(region.full));
            work.renderNorthRowChunks.or(BitSet.valueOf(region.rows));
            if (work.renderChunks.length() > 1024 || work.renderNorthRowChunks.length() > 1024 || work.renderChunks.intersects(work.renderNorthRowChunks)) {
                throw new IllegalArgumentException("Invalid region output masks");
            }
            work.completed = region.completed;
            regions.add(work);
        }
        if (regions.stream().map(RegionWork::coordinate).distinct().count() != regions.size()) {
            throw new IllegalArgumentException("Duplicate region ownership");
        }
        return new RenderPlan(regions);
    }

    /**
     * Removes work for chunks that are no longer visible. Never adds work.
     *
     * @param visible the current visibility predicate
     */
    void retain(final Predicate<ChunkCoordinate> visible) {
        for (final RegionWork work : this.regions) {
            retain(work, work.renderChunks, visible);
            retain(work, work.renderNorthRowChunks, visible);
        }
    }

    private static void retain(final RegionWork work, final BitSet chunks, final Predicate<ChunkCoordinate> visible) {
        for (int bit = chunks.nextSetBit(0); bit >= 0; bit = chunks.nextSetBit(bit + 1)) {
            if (!visible.test(work.chunk(bit))) {
                chunks.clear(bit);
            }
        }
    }

    record SavedRegion(RegionCoordinate coordinate, long[] full, long[] rows, boolean completed) {
    }

    public record ChunkWork(ChunkCoordinate coordinate, boolean topRowOnly) {
    }

    static final class RegionWork {
        private final RegionCoordinate coordinate;
        private final BitSet renderChunks = new BitSet(1024);
        private final BitSet renderNorthRowChunks = new BitSet(1024);
        private boolean completed;

        private RegionWork(final RegionCoordinate coordinate) {
            this.coordinate = coordinate;
        }

        RegionCoordinate coordinate() {
            return this.coordinate;
        }

        int chunkCount() {
            return this.renderChunks.cardinality();
        }

        boolean completed() {
            return this.completed;
        }

        void complete() {
            this.completed = true;
        }

        List<ChunkWork> chunks() {
            final List<ChunkWork> result = new ArrayList<>(this.renderChunks.cardinality() + this.renderNorthRowChunks.cardinality());
            for (int bit = this.renderChunks.nextSetBit(0); bit >= 0; bit = this.renderChunks.nextSetBit(bit + 1)) {
                result.add(new ChunkWork(this.chunk(bit), false));
            }
            for (int bit = this.renderNorthRowChunks.nextSetBit(0); bit >= 0; bit = this.renderNorthRowChunks.nextSetBit(bit + 1)) {
                result.add(new ChunkWork(this.chunk(bit), true));
            }
            return result;
        }

        List<List<ChunkWork>> columns(final int maximumChunks) {
            if (maximumChunks < 1) {
                throw new IllegalArgumentException("Column size must be positive");
            }
            final BitSet selected = (BitSet) this.renderChunks.clone();
            selected.or(this.renderNorthRowChunks);
            final List<List<ChunkWork>> columns = new ArrayList<>();
            List<ChunkWork> column = new ArrayList<>();
            int previous = -2;
            for (int bit = selected.nextSetBit(0); bit >= 0; bit = selected.nextSetBit(bit + 1)) {
                if (!column.isEmpty() && (bit != previous + 1 || bit / 32 != previous / 32 || column.size() == maximumChunks)) {
                    columns.add(List.copyOf(column));
                    column.clear();
                }
                column.add(new ChunkWork(this.chunk(bit), !this.renderChunks.get(bit)));
                previous = bit;
            }
            if (!column.isEmpty()) {
                columns.add(List.copyOf(column));
            }
            return columns;
        }

        private ChunkCoordinate chunk(final int bit) {
            return new ChunkCoordinate(this.coordinate.getChunkX() + bit / 32, this.coordinate.getChunkZ() + bit % 32);
        }
    }
}
