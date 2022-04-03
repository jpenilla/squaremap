package xyz.jpenilla.squaremap.sponge.listener;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.transaction.BlockTransactionReceipt;
import org.spongepowered.api.block.transaction.Operation;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.EventListenerRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.world.chunk.ChunkEvent;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.plugin.PluginContainer;
import xyz.jpenilla.squaremap.common.WorldManager;
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.sponge.config.SpongeAdvanced;
import xyz.jpenilla.squaremap.sponge.util.SpongeVectors;

@DefaultQualifier(NonNull.class)
public final class MapUpdateListener {
    private final Set<Object> registrations = new HashSet<>();
    private final WorldManager worldManager;
    private final PluginContainer pluginContainer;
    private final Game game;

    @Inject
    private MapUpdateListener(
        final WorldManager worldManager,
        final PluginContainer pluginContainer,
        final Game game
    ) {
        this.worldManager = worldManager;
        this.pluginContainer = pluginContainer;
        this.game = game;
    }

    public void register() {
        this.registerListeners(new BlockTriggers());
        this.registerListener(
            SpongeAdvanced.CHUNK_GENERATION,
            ChunkEvent.Generated.class,
            event -> this.mark(this.world(event.worldKey()), event.chunkPosition())
        );
        this.registerListener(
            SpongeAdvanced.CHUNK_LOAD,
            ChunkEvent.Load.class,
            event -> {
                if (event.chunk().world() instanceof ServerWorld world) {
                    this.mark(world, event.chunkPosition());
                }
            }
        );
    }

    public void unregister() {
        for (final Object listener : this.registrations) {
            this.game.eventManager().unregisterListeners(listener);
        }
    }

    private <E extends Event> void registerListener(final boolean enabled, final Class<E> clazz, final Consumer<E> consumer) {
        if (!enabled) {
            return;
        }
        this.registerListener(clazz, consumer);
    }

    private void registerListeners(final Object instance) {
        this.game.eventManager()
            .registerListeners(this.pluginContainer, instance);
        this.registrations.add(instance);
    }

    private <E extends Event> void registerListener(final Class<E> clazz, final Consumer<E> consumer) {
        final EventListener<E> listener = event -> {
            if (event instanceof Cancellable cancellable && cancellable.isCancelled()) {
                return;
            }
            consumer.accept(event);
        };
        this.game.eventManager().registerListener(
            EventListenerRegistration.builder(clazz)
                .plugin(this.pluginContainer)
                .listener(listener)
                .order(Order.POST)
                .build()
        );
        this.registrations.add(listener);
    }

    private ServerWorld world(final ResourceKey world) {
        return this.game.server().worldManager().world(world).orElseThrow();
    }

    private void mark(final ServerWorld level, final Vector3i chunk) {
        this.mark((ServerLevel) level, SpongeVectors.fromChunkPos(chunk));
    }

    private void mark(final ServerWorld level, final Collection<ChunkCoordinate> chunks) {
        this.worldManager.getWorldIfEnabled((ServerLevel) level).ifPresent(world -> {
            for (final ChunkCoordinate chunk : chunks) {
                world.chunkModified(chunk);
            }
        });
    }

    private void mark(final ServerLevel level, final ChunkCoordinate chunk) {
        this.worldManager.getWorldIfEnabled(level).ifPresent(world -> world.chunkModified(chunk));
    }

    public final class BlockTriggers {
        private final boolean disabled = Stream.of(SpongeAdvanced.BLOCK_PLACE, SpongeAdvanced.BLOCK_BREAK, SpongeAdvanced.BLOCK_MODIFY,
            SpongeAdvanced.BLOCK_GROWTH, SpongeAdvanced.BLOCK_DECAY, SpongeAdvanced.LIQUID_SPREAD, SpongeAdvanced.LIQUID_DECAY).noneMatch(b -> b);

        private BlockTriggers() {
        }

        @Listener(order = Order.POST)
        public void blockModify(final ChangeBlockEvent.Post event) {
            if (this.disabled) {
                return;
            }

            final Set<ChunkCoordinate> modified = new HashSet<>();
            for (final BlockTransactionReceipt receipt : event.receipts()) {
                final Operation operation = receipt.operation();
                if (operation == Operations.PLACE.get()) {
                    if (!SpongeAdvanced.BLOCK_PLACE) {
                        continue;
                    }
                } else if (operation == Operations.BREAK.get()) {
                    if (!SpongeAdvanced.BLOCK_BREAK) {
                        continue;
                    }
                } else if (operation == Operations.MODIFY.get()) {
                    if (!SpongeAdvanced.BLOCK_MODIFY) {
                        continue;
                    }
                } else if (operation == Operations.GROWTH.get()) {
                    if (!SpongeAdvanced.BLOCK_GROWTH) {
                        continue;
                    }
                } else if (operation == Operations.DECAY.get()) {
                    if (!SpongeAdvanced.BLOCK_DECAY) {
                        continue;
                    }
                } else if (operation == Operations.LIQUID_SPREAD.get()) {
                    if (!SpongeAdvanced.LIQUID_SPREAD) {
                        continue;
                    }
                } else if (operation == Operations.LIQUID_DECAY.get()) {
                    if (!SpongeAdvanced.LIQUID_DECAY) {
                        continue;
                    }
                } else {
                    throw new IllegalStateException("Unknown operation " + operation);
                }

                final ChunkCoordinate origChunk = SpongeVectors.fromBlockPos(receipt.originalBlock().position());
                final ChunkCoordinate finalChunk = SpongeVectors.fromBlockPos(receipt.finalBlock().position());
                if (origChunk.equals(finalChunk)) {
                    modified.add(finalChunk);
                } else {
                    modified.add(origChunk);
                    modified.add(finalChunk);
                }
            }
            if (!modified.isEmpty()) {
                MapUpdateListener.this.mark(event.world(), modified);
            }
        }
    }
}
