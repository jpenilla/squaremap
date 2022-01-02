package xyz.jpenilla.squaremap.common.util;

import java.util.Optional;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface BiomeSpecialEffectsAccess {
    Optional<Integer> grassColor(Biome biome);

    Optional<Integer> foliageColor(Biome biome);

    BiomeSpecialEffects.GrassColorModifier grassColorModifier(Biome biome);

    int waterColor(Biome biome);
}
