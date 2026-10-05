package xyz.jpenilla.squaremap.common.render.scanning;

import java.util.Arrays;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.chunksnapshot.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.coordinate.ChunkCoordinate;

/**
 * Reusable snapshot lookup for biomes. Caches the current chunk and its eight neighbors
 * for repeated lookups during scanning.
 */
@DefaultQualifier(NonNull.class)
final class BiomeSnapshotLookup implements BiomeColorSampler.SnapshotLookup {
    private final @Nullable ChunkSnapshot[] neighbors = new @Nullable ChunkSnapshot[9];
    private final boolean[] planned = new boolean[9];
    private Map<ChunkCoordinate, @Nullable ChunkSnapshot> inputs = Map.of();
    private int centerX;
    private int centerZ;

    void begin(final ChunkCoordinate center, final Map<ChunkCoordinate, @Nullable ChunkSnapshot> inputs) {
        this.inputs = inputs;
        this.centerX = center.x();
        this.centerZ = center.z();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                final int index = (dx + 1) * 3 + dz + 1;
                final ChunkCoordinate coordinate = new ChunkCoordinate(this.centerX + dx, this.centerZ + dz);
                this.planned[index] = inputs.containsKey(coordinate);
                this.neighbors[index] = inputs.get(coordinate);
            }
        }
    }

    @Override
    public @Nullable ChunkSnapshot get(final int x, final int z) {
        final int dx = x - this.centerX;
        final int dz = z - this.centerZ;
        if (dx >= -1 && dx <= 1 && dz >= -1 && dz <= 1) {
            final int index = (dx + 1) * 3 + dz + 1;
            if (!this.planned[index]) {
                throw new IllegalStateException("Unplanned biome dependency " + new ChunkCoordinate(x, z));
            }
            return this.neighbors[index];
        }
        final ChunkCoordinate coordinate = new ChunkCoordinate(x, z);
        if (!this.inputs.containsKey(coordinate)) {
            throw new IllegalStateException("Unplanned biome dependency " + coordinate);
        }
        return this.inputs.get(coordinate);
    }

    void clear() {
        this.inputs = Map.of();
        Arrays.fill(this.neighbors, null);
        Arrays.fill(this.planned, false);
    }
}
