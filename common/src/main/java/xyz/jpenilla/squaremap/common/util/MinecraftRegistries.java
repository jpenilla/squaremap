package xyz.jpenilla.squaremap.common.util;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import static java.util.Objects.requireNonNull;

@DefaultQualifier(NonNull.class)
public final class MinecraftRegistries {
    private MinecraftRegistries() {
    }

    public static <T> T requireEntry(final Registry<T> registry, final Identifier location) {
        // manually check for key, we don't want the default value if registry is a DefaultedRegistry
        if (!registry.containsKey(location)) {
            throw new IllegalArgumentException("No such entry '" + location + "' in registry '" + registry.key() + "'");
        }
        return requireNonNull(registry.getValue(location));
    }

    public static Registry<Biome> biomeRegistry(final CommonLevelAccessor level) {
        return biomeRegistry(level.registryAccess());
    }

    public static Registry<Biome> biomeRegistry(final RegistryAccess registryAccess) {
        return registryAccess.lookupOrThrow(Registries.BIOME);
    }
}
