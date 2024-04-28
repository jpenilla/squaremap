package xyz.jpenilla.squaremap.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.jar.Manifest;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

import static java.util.Objects.requireNonNull;

@DefaultQualifier(NonNull.class)
public final class Util {
    private static final Gson GSON = new GsonBuilder().create();

    private Util() {
    }

    public static Gson gson() {
        return GSON;
    }

    @SuppressWarnings("unchecked")
    public static <X extends Throwable> RuntimeException rethrow(final Throwable t) throws X {
        throw (X) t;
    }

    public static <T, X extends Throwable> Consumer<T> sneaky(final CheckedConsumer<T, X> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (final Throwable thr) {
                rethrow(thr);
            }
        };
    }

    public static ThreadFactory squaremapThreadFactory(final String name) {
        return new NamedThreadFactory("squaremap-" + name);
    }

    public static ThreadFactory squaremapThreadFactory(final String name, final ServerLevel level) {
        return squaremapThreadFactory(name + "-[" + level.dimension().location() + "]");
    }

    public static ThreadPoolExecutor newFixedThreadPool(final int size, final ThreadFactory threadFactory, final RejectedExecutionHandler rejectedExecutionHandler) {
        return new ThreadPoolExecutor(
            size,
            size,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            threadFactory,
            rejectedExecutionHandler
        );
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
        return registryAccess.registryOrThrow(Registries.BIOME);
    }

    public static @Nullable Manifest manifest(final Class<?> clazz) {
        final String classLocation = "/" + clazz.getName().replace(".", "/") + ".class";
        final @Nullable URL resource = clazz.getResource(classLocation);
        if (resource == null) {
            return null;
        }
        final String classFilePath = resource.toString().replace("\\", "/");
        final String archivePath = classFilePath.substring(0, classFilePath.length() - classLocation.length());
        try (final InputStream stream = URI.create(archivePath + "/META-INF/MANIFEST.MF").toURL().openStream()) {
            return new Manifest(stream);
        } catch (final IOException ex) {
            return null;
        }
    }
}
