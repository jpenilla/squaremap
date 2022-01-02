package xyz.jpenilla.squaremap.plugin.util;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.squaremap.common.util.BiomeSpecialEffectsAccess;
import xyz.jpenilla.squaremap.common.util.ReflectionUtil;

public final class CraftBukkitReflection {
    private CraftBukkitReflection() {
    }

    private static final BiomeSpecialEffectsProxy BIOME_SPECIAL_EFFECTS;
    private static final String PREFIX_CRAFTBUKKIT = "org.bukkit.craftbukkit";
    private static final String CRAFT_SERVER = "CraftServer";
    private static final String CB_PKG_VERSION;

    static {
        final Class<?> serverClass = Bukkit.getServer().getClass();
        String name = serverClass.getName();
        name = name.substring(PREFIX_CRAFTBUKKIT.length());
        name = name.substring(0, name.length() - CRAFT_SERVER.length());
        CB_PKG_VERSION = name;

        final ReflectionRemapper reflectionRemapper = ReflectionRemapper.forReobfMappingsInPaperJar();
        final ReflectionProxyFactory factory = ReflectionProxyFactory.create(reflectionRemapper, BiomeSpecialEffectsHelper.class.getClassLoader());

        BIOME_SPECIAL_EFFECTS = factory.reflectionProxy(BiomeSpecialEffectsProxy.class);
    }

    public static @NonNull Class<?> needOBCClass(final @NonNull String className) {
        return ReflectionUtil.needClass(PREFIX_CRAFTBUKKIT + CB_PKG_VERSION + className);
    }

    private static final Class<?> CRAFT_WORLD_CLASS = needOBCClass("CraftWorld");
    private static final Method CRAFT_WORLD_GET_HANDLE = ReflectionUtil.needMethod(CRAFT_WORLD_CLASS, List.of("getHandle"));

    public static @NonNull ServerLevel serverLevel(final @NonNull World world) {
        try {
            return (ServerLevel) CRAFT_WORLD_GET_HANDLE.invoke(world);
        } catch (final ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Proxies(BiomeSpecialEffects.class)
    private interface BiomeSpecialEffectsProxy {
        @FieldGetter("grassColorOverride")
        Optional<Integer> grassColorOverride(BiomeSpecialEffects effects);

        @FieldGetter("foliageColorOverride")
        Optional<Integer> foliageColorOverride(BiomeSpecialEffects effects);

        @FieldGetter("grassColorModifier")
        BiomeSpecialEffects.GrassColorModifier grassColorModifier(BiomeSpecialEffects effects);

        @FieldGetter("waterColor")
        int waterColor(BiomeSpecialEffects effects);
    }

    @DefaultQualifier(NonNull.class)
    public static final class BiomeSpecialEffectsHelper implements BiomeSpecialEffectsAccess {
        private static final BiomeSpecialEffectsHelper INSTANCE = new BiomeSpecialEffectsHelper();

        private BiomeSpecialEffectsHelper() {
        }

        @Override
        public Optional<Integer> grassColor(final Biome biome) {
            return BIOME_SPECIAL_EFFECTS.grassColorOverride(biome.getSpecialEffects());
        }

        @Override
        public Optional<Integer> foliageColor(final Biome biome) {
            return BIOME_SPECIAL_EFFECTS.foliageColorOverride(biome.getSpecialEffects());
        }

        @Override
        public BiomeSpecialEffects.GrassColorModifier grassColorModifier(final Biome biome) {
            return BIOME_SPECIAL_EFFECTS.grassColorModifier(biome.getSpecialEffects());
        }

        @Override
        public int waterColor(final Biome biome) {
            return BIOME_SPECIAL_EFFECTS.waterColor(biome.getSpecialEffects());
        }

        public static BiomeSpecialEffectsHelper get() {
            return INSTANCE;
        }
    }
}
