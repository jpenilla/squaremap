package xyz.jpenilla.squaremap.fabric.util;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;

@DefaultQualifier(NonNull.class)
public final class FabricMapUpdates {
    private FabricMapUpdates() {
    }

    public static void registerListeners() {
        PlayerBlockBreakEvents.AFTER.register(FabricMapUpdates::afterBlockBreak);
    }

    public static void mark(final ServerLevel level, final BlockPos pos) {
        mark(level, pos, false);
    }

    public static void mark(final ServerLevel level, final BlockPos pos, final boolean skipVisibilityCheck) {
        if (skipVisibilityCheck
            || level.dimensionType().hasCeiling()) { // heightmap will just give us the roof
            mark(level, new ChunkCoordinate(pos.getX() >> 4, pos.getZ() >> 4));
            return;
        }

        final int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        if (pos.getY() >= height - 10) {
            mark(level, new ChunkCoordinate(pos.getX() >> 4, pos.getZ() >> 4));
        }
    }

    public static void mark(final ServerLevel level, final ChunkCoordinate chunk) {
        SquaremapCommon.instance().platform().worldManager()
            .getWorldIfEnabled(level)
            .ifPresent(world -> world.chunkModified(chunk));
    }

    private static void afterBlockBreak(
        final Level level,
        final Player player,
        final BlockPos blockPos,
        final BlockState blockState,
        final @Nullable BlockEntity blockEntity
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        mark(serverLevel, blockPos);
    }
}
