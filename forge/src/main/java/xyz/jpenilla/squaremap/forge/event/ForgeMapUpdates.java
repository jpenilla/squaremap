package xyz.jpenilla.squaremap.forge.event;

import com.google.inject.Inject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.forge.ForgeWorldManager;

@DefaultQualifier(NonNull.class)
public final class ForgeMapUpdates {
    private final ForgeWorldManager worldManager;

    @Inject
    private ForgeMapUpdates(final ForgeWorldManager worldManager) {
        this.worldManager = worldManager;
    }

    public void register() {
        MinecraftForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
            if (!(event.getLevel() instanceof ServerLevel level)) {
                return;
            }
            this.markBlock(level, event.getPos());
        });
        MinecraftForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event) -> {
            if (!(event.getLevel() instanceof ServerLevel level)) {
                return;
            }
            this.markBlock(level, event.getPos());
        });
        MinecraftForge.EVENT_BUS.addListener((ChunkGenerateEvent event) -> this.markChunk(event.level(), event.chunkPos()));
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
