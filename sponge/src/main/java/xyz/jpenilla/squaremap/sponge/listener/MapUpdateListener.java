package xyz.jpenilla.squaremap.sponge.listener;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
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
import xyz.jpenilla.squaremap.common.data.ChunkCoordinate;
import xyz.jpenilla.squaremap.sponge.SquaremapSponge;
import xyz.jpenilla.squaremap.sponge.config.SpongeAdvanced;
import xyz.jpenilla.squaremap.sponge.util.SpongeVectors;

@DefaultQualifier(NonNull.class)
public final class MapUpdateListener {
    private final Set<Object> registrations = new HashSet<>();
    private final SquaremapSponge squaremapSponge;

    public MapUpdateListener(final SquaremapSponge squaremapSponge) {
        this.squaremapSponge = squaremapSponge;
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
            this.squaremapSponge.game().eventManager().unregisterListeners(listener);
        }
    }

    private <E extends Event> void registerListener(final String toggleKey, final Class<E> clazz, final Consumer<E> consumer) {
        if (!SpongeAdvanced.listenerEnabled(toggleKey)) {
            return;
        }
        this.registerListener(clazz, consumer);
    }

    private void registerListeners(final Object instance) {
        this.squaremapSponge.game().eventManager()
            .registerListeners(this.squaremapSponge.pluginContainer(), instance);
        this.registrations.add(instance);
    }

    private <E extends Event> void registerListener(final Class<E> clazz, final Consumer<E> consumer) {
        final EventListener<E> listener = event -> {
            if (event instanceof Cancellable cancellable && cancellable.isCancelled()) {
                return;
            }
            consumer.accept(event);
        };
        this.squaremapSponge.game().eventManager().registerListener(
            EventListenerRegistration.builder(clazz)
                .plugin(this.squaremapSponge.pluginContainer())
                .listener(listener)
                .order(Order.POST)
                .build()
        );
        this.registrations.add(listener);
    }

    private ServerWorld world(final ResourceKey world) {
        return this.squaremapSponge.game().server().worldManager().world(world).orElseThrow();
    }

    private void mark(final ServerWorld level, final Vector3i chunk) {
        this.mark((ServerLevel) level, SpongeVectors.fromChunkPos(chunk));
    }

    private void mark(final ServerWorld level, final Collection<ChunkCoordinate> chunks) {
        this.squaremapSponge.worldManager()
            .getWorldIfEnabled((ServerLevel) level)
            .ifPresent(world -> {
                for (final ChunkCoordinate chunk : chunks) {
                    world.chunkModified(chunk);
                }
            });
    }

    private void mark(final ServerLevel level, final ChunkCoordinate chunk) {
        this.squaremapSponge.worldManager()
            .getWorldIfEnabled(level)
            .ifPresent(world -> world.chunkModified(chunk));
    }

    public final class BlockTriggers {
        private final boolean blockPlace = SpongeAdvanced.listenerEnabled(SpongeAdvanced.BLOCK_PLACE);
        private final boolean blockBreak = SpongeAdvanced.listenerEnabled(SpongeAdvanced.BLOCK_BREAK);
        private final boolean blockModify = SpongeAdvanced.listenerEnabled(SpongeAdvanced.BLOCK_MODIFY);
        private final boolean blockGrowth = SpongeAdvanced.listenerEnabled(SpongeAdvanced.BLOCK_GROWTH);
        private final boolean blockDecay = SpongeAdvanced.listenerEnabled(SpongeAdvanced.BLOCK_DECAY);

        private final boolean liquidSpread = SpongeAdvanced.listenerEnabled(SpongeAdvanced.LIQUID_SPREAD);
        private final boolean liquidDecay = SpongeAdvanced.listenerEnabled(SpongeAdvanced.LIQUID_DECAY);

        private final boolean disabled = Stream.of(this.blockPlace, this.blockBreak, this.blockModify,
            this.blockGrowth, this.blockDecay, this.liquidSpread, this.liquidDecay).noneMatch(b -> b);

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
                    if (!this.blockPlace) {
                        continue;
                    }
                } else if (operation == Operations.BREAK.get()) {
                    if (!this.blockBreak) {
                        continue;
                    }
                } else if (operation == Operations.MODIFY.get()) {
                    if (!this.blockModify) {
                        continue;
                    }
                } else if (operation == Operations.GROWTH.get()) {
                    if (!this.blockGrowth) {
                        continue;
                    }
                } else if (operation == Operations.DECAY.get()) {
                    if (!this.blockDecay) {
                        continue;
                    }
                } else if (operation == Operations.LIQUID_SPREAD.get()) {
                    if (!this.liquidSpread) {
                        continue;
                    }
                } else if (operation == Operations.LIQUID_DECAY.get()) {
                    if (!this.liquidDecay) {
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
