package xyz.jpenilla.squaremap.fabric.listener;

import com.google.inject.Inject;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.WorldManager;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.fabric.event.MapUpdateEvents;

@DefaultQualifier(NonNull.class)
public final class FabricMapUpdates {
    private final WorldManager worldManager;

    @Inject
    private FabricMapUpdates(final WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    public void register() {
        // listen to fabric api events
        PlayerBlockBreakEvents.AFTER.register(this::afterBlockBreak);

        // these are generic events fired by squaremap's mixins, covering cases
        // where there isn't a fabric api event
        MapUpdateEvents.BLOCK_CHANGED.register(this::markBlock);
        MapUpdateEvents.CHUNK_CHANGED.register(this::markChunk);
    }

    private void afterBlockBreak(
        final Level level,
        final Player player,
        final BlockPos blockPos,
        final BlockState blockState,
        final @Nullable BlockEntity blockEntity
    ) {
        if (level instanceof ServerLevel serverLevel) {
            this.markBlock(serverLevel, blockPos);
        }
    }

    private void markBlock(final ServerLevel level, final BlockPos pos) {
        this.markBlock(level, pos, false);
    }

    private void markBlock(final ServerLevel level, final BlockPos pos, final boolean skipVisibilityCheck) {
        if (skipVisibilityCheck
            || level.dimensionType().hasCeiling()) { // heightmap will just give us the roof
            this.markChunk(level, new ChunkCoordinate(pos.getX() >> 4, pos.getZ() >> 4));
            return;
        }

        final int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        if (pos.getY() >= height - 10) {
            this.markChunk(level, new ChunkCoordinate(pos.getX() >> 4, pos.getZ() >> 4));
        }
    }

    private void markChunk(final ServerLevel level, final ChunkPos chunk) {
        this.markChunk(level, new ChunkCoordinate(chunk.x, chunk.z));
    }

    private void markChunk(final ServerLevel level, final ChunkCoordinate chunk) {
        this.worldManager.getWorldIfEnabled(level).ifPresent(world -> world.chunkModified(chunk));
    }
}
