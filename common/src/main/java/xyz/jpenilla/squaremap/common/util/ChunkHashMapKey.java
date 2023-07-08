package xyz.jpenilla.squaremap.common.util;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.world.level.ChunkPos;

public final class ChunkHashMapKey implements Comparable<ChunkHashMapKey> {
    public final long key;

    public ChunkHashMapKey(final long key) {
        this.key = key;
    }

    public ChunkHashMapKey(final ChunkPos pos) {
        this(pos.toLong());
    }

    @Override
    public int hashCode() {
        return (int) HashCommon.mix(this.key);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof final ChunkHashMapKey other)) {
            return false;
        }

        return this.key == other.key;
    }

    @Override
    public int compareTo(final ChunkHashMapKey other) {
        return Long.compare(this.key, other.key);
    }

    @Override
    public String toString() {
        return new ChunkPos(this.key).toString();
    }
}
