package xyz.jpenilla.squaremap.plugin.util;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface ChunkSnapshot extends LevelHeightAccessor, BiomeManager.NoiseBiomeSource {
    BlockState getBlockState(BlockPos pos);

    FluidState getFluidState(BlockPos pos);

    int getHeight(Heightmap.Types type, int x, int z);

    Biome getBiome(BlockPos pos);

    DimensionType dimensionType();

    ChunkPos pos();

    boolean sectionEmpty(int sectionIndex);

    static CompletableFuture<@Nullable ChunkSnapshot> asyncSnapshot(final ServerLevel level, final int x, final int z) {
        return level.getChunkSource().getChunkAtAsynchronously(x, z, false, true)
            .thenApply(either -> either.left()
                .map(chunk -> {
                    final LevelChunk levelChunk = (LevelChunk) chunk;
                    if (levelChunk.isEmpty()) {
                        return null;
                    }
                    return ChunkSnapshot.snapshot(levelChunk);
                })
                .orElse(null));
    }

    @SuppressWarnings({"unchecked", "deprecation", "rawtypes"})
    static ChunkSnapshot snapshot(final LevelChunk chunk) {
        // AsyncCatcher.catchOp("Chunk Snapshot");
        final LevelChunkSection[] sections = chunk.getSections();
        final PalettedContainer<BlockState>[] states = new PalettedContainer[sections.length];
        final PalettedContainer<Biome>[] biomes = new PalettedContainer[sections.length];

        final Registry<Biome> biomeRegistry = chunk.level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        final Codec<PalettedContainer<Biome>> biomeCodec = PalettedContainer.codec(
            biomeRegistry,
            biomeRegistry.byNameCodec(),
            PalettedContainer.Strategy.SECTION_BIOMES,
            biomeRegistry.getOrThrow(Biomes.PLAINS)
        );

        final boolean[] empty = new boolean[sections.length];

        for (int i = 0; i < sections.length; i++) {
            empty[i] = sections[i].hasOnlyAir();

            states[i] = ChunkSerializer.BLOCK_STATE_CODEC.parse(
                NbtOps.INSTANCE,
                ChunkSerializer.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, sections[i].getStates())
                    .get().left().orElseThrow()
            ).get().left().orElseThrow();

            biomes[i] = biomeCodec.parse(
                NbtOps.INSTANCE,
                biomeCodec.encodeStart(NbtOps.INSTANCE, sections[i].getBiomes())
                    .get().left().orElseThrow()
            ).get().left().orElseThrow();
        }

        final Heightmap heightmap = new Heightmap(chunk, Heightmap.Types.WORLD_SURFACE);
        heightmap.setRawData(chunk, Heightmap.Types.WORLD_SURFACE, chunk.heightmaps.get(Heightmap.Types.WORLD_SURFACE).getRawData());

        return new ChunkSnapshotImpl(
            LevelHeightAccessor.create(chunk.getMinBuildHeight(), chunk.getMaxBuildHeight()),
            states,
            biomes,
            Util.make(new EnumMap<>(Heightmap.Types.class), map -> map.put(Heightmap.Types.WORLD_SURFACE, heightmap)),
            empty,
            chunk.level.dimensionType(),
            chunk.level.getSeed(),
            chunk.getPos()
        );
    }
}
