package xyz.jpenilla.squaremap.common.chunksnapshot;

import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum ChunkSnapshotEligibility {
    INELIGIBLE,
    ELIGIBLE,
    NEEDS_RETROGEN;

    public static ChunkSnapshotEligibility get(final ChunkStatus status, final @Nullable BelowZeroRetrogen retroGen) {
        if (retroGen == null) {
            return status.isOrAfter(ChunkStatus.FULL) ? ELIGIBLE : INELIGIBLE;
        }
        if (status.isOrAfter(ChunkStatus.FULL) || retroGen.targetStatus().isOrAfter(ChunkStatus.FULL)) {
            return NEEDS_RETROGEN;
        }
        // pre-1.18 FULL chunks get a retrogen target of HEIGHTMAPS, later remapped to SPAWN
        // there is no way to distinguish a true pre-1.18 SPAWN chunk from a pre-1.18 FULL chunk
        return retroGen.targetStatus().isOrAfter(ChunkStatus.SPAWN)
            ? NEEDS_RETROGEN : INELIGIBLE;
    }
}
