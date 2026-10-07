package xyz.jpenilla.squaremap.common.render.scanning;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.junit.jupiter.api.Test;
import xyz.jpenilla.squaremap.common.chunksnapshot.ChunkSnapshot;
import xyz.jpenilla.squaremap.common.coordinate.ChunkCoordinate;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BiomeSnapshotLookupTest {
    @Test
    void resolvesNearbyAndDistantDependenciesAtNegativeCoordinates() {
        final ChunkCoordinate center = new ChunkCoordinate(-32, -1);
        final BiomeSnapshotLookup lookup = new BiomeSnapshotLookup();
        final Map<ChunkCoordinate, ChunkSnapshot> inputs = Map.of(
            center, new TestSnapshot(),
            new ChunkCoordinate(-33, -2), new TestSnapshot(),
            new ChunkCoordinate(-31, 0), new TestSnapshot(),
            new ChunkCoordinate(-35, -4), new TestSnapshot()
        );
        lookup.begin(center, inputs);
        inputs.forEach((coordinate, snapshot) -> assertSame(snapshot, lookup.get(coordinate.x(), coordinate.z())));
        assertThrows(IllegalStateException.class, () -> lookup.get(-32, 0));
        assertThrows(IllegalStateException.class, () -> lookup.get(-34, -3));
    }

    @Test
    void reuseClearsOldDependenciesWithoutConfusingNullAndUnplannedEntries() {
        final BiomeSnapshotLookup lookup = new BiomeSnapshotLookup();
        final ChunkCoordinate center = new ChunkCoordinate(0, 0);
        final Map<ChunkCoordinate, @Nullable ChunkSnapshot> inputs = new HashMap<>();
        inputs.put(center, new TestSnapshot());
        inputs.put(new ChunkCoordinate(1, 1), null);
        inputs.put(new ChunkCoordinate(3, 3), null);
        lookup.begin(center, inputs);
        assertSame(inputs.get(center), lookup.get(0, 0));
        assertNull(lookup.get(1, 1));
        assertNull(lookup.get(3, 3));
        assertThrows(IllegalStateException.class, () -> lookup.get(-1, -1));
        assertThrows(IllegalStateException.class, () -> lookup.get(-3, -3));

        lookup.clear();
        assertThrows(IllegalStateException.class, () -> lookup.get(0, 0));
        assertThrows(IllegalStateException.class, () -> lookup.get(3, 3));

        final ChunkSnapshot replacement = new TestSnapshot();
        lookup.begin(new ChunkCoordinate(-1, -1), Map.of(center, replacement));
        assertSame(replacement, lookup.get(0, 0));
        assertThrows(IllegalStateException.class, () -> lookup.get(1, 1));
        lookup.clear();
    }

    /**
     * Provides distinct snapshot instances for lookup assertions. Snapshot data is never read,
     * so the tests do not need to construct Minecraft chunk state.
     */
    @DefaultQualifier(NonNull.class)
    private static final class TestSnapshot implements ChunkSnapshot {
        @Override public BlockState getBlockState(final BlockPos pos) { throw new UnsupportedOperationException(); }
        @Override public FluidState getFluidState(final BlockPos pos) { throw new UnsupportedOperationException(); }
        @Override public int getHeight(final Heightmap.Types type, final int x, final int z) { throw new UnsupportedOperationException(); }
        @Override public DimensionType dimensionType() { throw new UnsupportedOperationException(); }
        @Override public ChunkPos pos() { throw new UnsupportedOperationException(); }
        @Override public boolean sectionEmpty(final int index) { throw new UnsupportedOperationException(); }
        @Override public int getHeight() { throw new UnsupportedOperationException(); }
        @Override public int getMinY() { throw new UnsupportedOperationException(); }
        @Override public Holder<Biome> getNoiseBiome(final int quartX, final int quartY, final int quartZ) { throw new UnsupportedOperationException(); }
        @Override public Holder<Biome> uniformBiome(final int y) { throw new UnsupportedOperationException(); }
    }
}
