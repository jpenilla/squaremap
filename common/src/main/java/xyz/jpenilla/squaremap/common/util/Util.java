package xyz.jpenilla.squaremap.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

import static java.util.Objects.requireNonNull;

@DefaultQualifier(NonNull.class)
public final class Util {
    private Util() {
    }

    @SuppressWarnings("unchecked")
    public static <X extends Throwable> RuntimeException rethrow(final Throwable t) throws X {
        throw (X) t;
    }

    public static ThreadFactory squaremapThreadFactory(final String name) {
        return new NamedThreadFactory("squaremap-" + name);
    }

    public static ThreadFactory squaremapThreadFactory(final String name, final ServerLevel level) {
        return squaremapThreadFactory(name + "-[" + level.dimension().location() + "]");
    }

    public static void shutdownExecutor(final ExecutorService service, final TimeUnit timeoutUnit, final long timeoutLength) {
        service.shutdown();
        boolean didShutdown;
        try {
            didShutdown = service.awaitTermination(timeoutLength, timeoutUnit);
        } catch (final InterruptedException ignore) {
            didShutdown = false;
        }
        if (!didShutdown) {
            service.shutdownNow();
        }
    }

    public static <T> T requireEntry(final Registry<T> registry, final ResourceLocation location) {
        // manually check for key, we don't want the default value if registry is a DefaultedRegistry
        if (!registry.containsKey(location)) {
            throw new IllegalArgumentException("No such entry '" + location + "' in registry '" + registry.key() + "'");
        }
        return requireNonNull(registry.get(location));
    }

    public static String levelConfigName(final ServerLevel level) {
        return level.dimension().location().toString();
    }

    public static String levelWebName(final ServerLevel level) {
        return level.dimension().location().toString().replace(":", "_");
    }

    public static WorldIdentifier worldIdentifier(final ServerLevel level) {
        final ResourceLocation location = level.dimension().location();
        return worldIdentifier(location);
    }

    public static WorldIdentifier worldIdentifier(final ResourceLocation location) {
        return WorldIdentifier.create(location.getNamespace(), location.getPath());
    }

    public static byte[] raw(final FriendlyByteBuf buf) {
        final byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    public static Registry<Biome> biomeRegistry(final CommonLevelAccessor level) {
        return biomeRegistry(level.registryAccess());
    }

    public static Registry<Biome> biomeRegistry(final RegistryAccess registryAccess) {
        return registryAccess.ownedRegistryOrThrow(Registry.BIOME_REGISTRY);
    }
}
