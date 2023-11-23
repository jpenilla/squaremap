package xyz.jpenilla.squaremap.forge.event;

import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.SaplingGrowTreeEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.WorldManagerImpl;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;

@DefaultQualifier(NonNull.class)
public final class ForgeMapUpdates {
    private final WorldManagerImpl worldManager;

    @Inject
    private ForgeMapUpdates(final WorldManagerImpl worldManager) {
        this.worldManager = worldManager;
    }

    public void register() {
        List.of(
            BlockEvent.BreakEvent.class,
            BlockEvent.EntityPlaceEvent.class,
            BlockEvent.CropGrowEvent.Post.class,
            BlockEvent.BlockToolModificationEvent.class,
            BlockEvent.EntityMultiPlaceEvent.class,
            BlockEvent.FarmlandTrampleEvent.class,
            BlockEvent.FluidPlaceBlockEvent.class,
            BlockEvent.PortalSpawnEvent.class
        ).forEach(cls -> NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, cls, event -> {
            if (!(event.getLevel() instanceof ServerLevel level)) {
                return;
            }
            if (event instanceof BlockEvent.EntityMultiPlaceEvent multiPlace) {
                multiPlace.getReplacedBlockSnapshots().stream().map(BlockSnapshot::getPos)
                    .forEach(pos -> this.markBlock(level, event.getPos()));
            }
            this.markBlock(level, event.getPos());
        }));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (BlockEvent.CreateFluidSourceEvent event) -> {
            if (!(event.getLevel() instanceof ServerLevel level)) {
                return;
            }
            this.markBlock(level, event.getPos());
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (ExplosionEvent.Detonate event) -> {
            if (!(event.getLevel() instanceof ServerLevel level)) {
                return;
            }
            for (final BlockPos affectedBlock : event.getAffectedBlocks()) {
                this.markBlock(level, affectedBlock);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (SaplingGrowTreeEvent event) -> {
            if (!(event.getLevel() instanceof ServerLevel level)) {
                return;
            }
            this.markBlock(level, event.getPos());
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (PistonEvent.Post event) -> {
            if (!(event.getLevel() instanceof ServerLevel level)) {
                return;
            }
            final PistonStructureResolver structureHelper = Objects.requireNonNull(event.getStructureHelper());
            Stream.concat(structureHelper.getToPush().stream(), structureHelper.getToDestroy().stream())
                .forEach(pos -> this.markBlock(level, pos));
            this.markBlock(level, event.getPos());
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, (ChunkEvent.Load event) -> {
            if (!(event.getLevel() instanceof ServerLevel level) || !event.isNewChunk()) {
                return;
            }
            this.markChunk(level, event.getChunk().getPos());
        });
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
