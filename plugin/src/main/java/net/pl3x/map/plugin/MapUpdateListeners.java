package net.pl3x.map.plugin;

import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.data.ChunkCoordinate;
import net.pl3x.map.plugin.util.Numbers;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

final class MapUpdateListeners {

    private final Pl3xMap plugin;

    MapUpdateListeners(final @NonNull Pl3xMap plugin) {
        this.plugin = plugin;
    }

    public void register() {
        this.registerBlockEventListener(BlockPlaceEvent.class);
        this.registerBlockEventListener(BlockBreakEvent.class);
        this.registerBlockEventListener(LeavesDecayEvent.class);
        this.registerBlockEventListener(BlockBurnEvent.class);
        this.registerBlockEventListener(BlockExplodeEvent.class);
        this.registerBlockEventListener(BlockGrowEvent.class);
        this.registerBlockEventListener(BlockFormEvent.class);
        this.registerBlockEventListener(EntityBlockFormEvent.class);
        this.registerBlockEventListener(BlockSpreadEvent.class);
        this.registerBlockEventListener(BlockPhysicsEvent.class);

        this.registerPlayerEventListener(PlayerMoveEvent.class);
        this.registerPlayerEventListener(PlayerJoinEvent.class);
        this.registerPlayerEventListener(PlayerQuitEvent.class);

        this.registerListener(BlockPistonExtendEvent.class, this::handleBlockPistonExtendEvent);
        this.registerListener(BlockPistonRetractEvent.class, this::handleBlockPistonRetractEvent);
        this.registerListener(ChunkPopulateEvent.class, this::handleChunkPopulateEvent);
        this.registerListener(FluidLevelChangeEvent.class, this::handleFluidLevelChangeEvent);
        this.registerListener(BlockFromToEvent.class, this::handleBlockFromToEvent);
        this.registerListener(EntityExplodeEvent.class, this::handleEntityExplodeEvent);
        this.registerListener(EntityChangeBlockEvent.class, this::handleEntityChangeBlockEvent);
        this.registerListener(StructureGrowEvent.class, this::handleStructureGrowEvent);
    }

    private <E extends Event> void registerListener(final @NonNull Class<E> eventClass, final @NonNull Consumer<E> listener) {
        if (!Config.listenerEnabled(eventClass)) {
            return;
        }
        Bukkit.getPluginManager().registerEvent(
                eventClass,
                new Listener() {
                },
                EventPriority.MONITOR,
                (l, event) -> {
                    if (!eventClass.isAssignableFrom(event.getClass())) {
                        return;
                    }
                    listener.accept(eventClass.cast(event));
                },
                this.plugin,
                true
        );
    }

    private <B extends BlockEvent> void registerBlockEventListener(final @NonNull Class<B> eventClass) {
        this.registerListener(eventClass, this::handleBlockEvent);
    }

    private <P extends PlayerEvent> void registerPlayerEventListener(final @NonNull Class<P> eventClass) {
        this.registerListener(eventClass, this::handlePlayerEvent);
    }

    private void markChunk(final @NonNull Location loc) {
        this.markChunk(loc, false);
    }

    private void markChunk(final @NonNull Location loc, final boolean ignoreCeiling) {
        WorldManager.getWorldIfEnabled(loc.getWorld()).ifPresent(mapWorld -> {
            if (ignoreCeiling || loc.getY() >= loc.getWorld().getHighestBlockYAt(loc) - 10) {
                mapWorld.chunkModified(new ChunkCoordinate(
                        Numbers.blockToChunk(loc.getBlockX()),
                        Numbers.blockToChunk(loc.getBlockZ())
                ));
            }
        });
    }

    private void markLocations(final @NonNull World world, final @NonNull List<Location> locations) {
        WorldManager.getWorldIfEnabled(world).ifPresent(mapWorld -> locations.stream()
                .map(loc -> new ChunkCoordinate(
                        Numbers.blockToChunk(loc.getBlockX()),
                        Numbers.blockToChunk(loc.getBlockZ())
                ))
                .distinct()
                .forEach(mapWorld::chunkModified));
    }

    private void markChunksFromBlocks(final @NonNull World world, final @NonNull List<BlockState> blockStates) {
        WorldManager.getWorldIfEnabled(world).ifPresent(mapWorld ->
                blockStates.stream()
                        .map(BlockState::getLocation)
                        .filter(loc -> loc.getY() >= world.getHighestBlockYAt(loc) - 10)
                        .map(loc -> new ChunkCoordinate(
                                Numbers.blockToChunk(loc.getBlockX()),
                                Numbers.blockToChunk(loc.getBlockZ())
                        ))
                        .distinct()
                        .forEach(mapWorld::chunkModified));
    }

    private void handleBlockPistonExtendEvent(final @NonNull BlockPistonExtendEvent event) {
        this.markLocations(event.getBlock().getWorld(), event.getBlocks().stream().map(Block::getLocation).collect(Collectors.toList()));
    }

    private void handleBlockPistonRetractEvent(final @NonNull BlockPistonRetractEvent event) {
        this.markLocations(event.getBlock().getWorld(), event.getBlocks().stream().map(Block::getLocation).collect(Collectors.toList()));
    }

    private void handleChunkPopulateEvent(final @NonNull ChunkPopulateEvent event) {
        final Chunk chunk = event.getChunk();
        this.markChunk(new Location(chunk.getWorld(), Numbers.chunkToBlock(chunk.getX()), 0, Numbers.chunkToBlock(chunk.getZ())));
    }

    private void handleBlockEvent(final @NonNull BlockEvent blockEvent) {
        this.markChunk(blockEvent.getBlock().getLocation());
    }

    private void handlePlayerEvent(final @NonNull PlayerEvent playerEvent) {
        this.markChunk(playerEvent.getPlayer().getLocation());
    }

    private void handleStructureGrowEvent(final @NonNull StructureGrowEvent event) {
        this.markChunksFromBlocks(event.getWorld(), event.getBlocks());
    }

    private void handleBlockFromToEvent(final @NonNull BlockFromToEvent event) {
        //this.handleBlockEvent(event);
        //this.markChunk(event.getToBlock().getLocation());
        // this event gets spammed really hard, to the point where checking the highest Y becomes quite expensive.
        // it's better to queue some unnecessary map updates than to cause tps lag if this listener is enabled.
        this.markChunk(event.getToBlock().getLocation(), true);
    }

    private void handleEntityChangeBlockEvent(final @NonNull EntityChangeBlockEvent event) {
        this.markChunk(event.getBlock().getLocation());
    }

    private void handleEntityExplodeEvent(final @NonNull EntityExplodeEvent event) {
        this.markChunksFromBlocks(event.getLocation().getWorld(), event.blockList().stream().map(Block::getState).collect(Collectors.toList()));
    }

    private void handleFluidLevelChangeEvent(final @NonNull FluidLevelChangeEvent event) {
        if (event.getBlock().getBlockData().getMaterial() != event.getNewData().getMaterial()) {
            this.handleBlockEvent(event);
        }
    }
}
