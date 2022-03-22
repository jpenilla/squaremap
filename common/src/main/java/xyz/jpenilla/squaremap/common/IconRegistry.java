package xyz.jpenilla.squaremap.common;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Pair;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.FileUtil;

@DefaultQualifier(NonNull.class)
public final class IconRegistry implements Registry<BufferedImage> {
    private final Map<Key, BufferedImage> images = new ConcurrentHashMap<>();
    private final Path directory;

    public IconRegistry(final DirectoryProvider directoryProvider) {
        this.directory = directoryProvider.webDirectory().resolve("images/icon/registered/");
        try {
            if (Files.exists(this.directory)) {
                FileUtil.deleteRecursively(this.directory);
            }
            Files.createDirectories(this.directory);
        } catch (final IOException e) {
            throw failedToCreateRegistry(e);
        }
    }

    @Override
    public void register(final Key key, final BufferedImage value) {
        if (this.hasEntry(key)) {
            throw imageAlreadyRegistered(key);
        }
        try {
            ImageIO.write(value, "png", this.directory.resolve(key.getKey() + ".png").toFile());
        } catch (IOException e) {
            throw failedToWriteImage(key, e);
        }
        this.images.put(key, value);
    }

    @Override
    public void unregister(final Key key) {
        final BufferedImage removed = this.images.remove(key);
        if (removed == null) {
            throw noImageRegistered(key);
        }
        try {
            Files.delete(this.directory.resolve(key.getKey() + ".png"));
        } catch (IOException e) {
            throw failedToDeleteImage(key, e);
        }
    }

    @Override
    public boolean hasEntry(final Key key) {
        return this.images.containsKey(key);
    }

    @Override
    public BufferedImage get(final Key key) {
        final BufferedImage provider = this.images.get(key);
        if (provider == null) {
            throw noImageRegistered(key);
        }
        return provider;
    }

    @Override
    public Iterable<Pair<Key, BufferedImage>> entries() {
        return this.images.entrySet().stream()
            .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
            .toList();
    }

    private static IllegalArgumentException failedToCreateRegistry(final IOException e) {
        return new IllegalArgumentException("Failed to setup icon registry", e);
    }

    private static IllegalArgumentException failedToDeleteImage(final Key key, final IOException e) {
        return new IllegalArgumentException(String.format("Failed to delete image for key '%s'", key.getKey()), e);
    }

    private static IllegalArgumentException failedToWriteImage(final Key key, final IOException e) {
        return new IllegalArgumentException(String.format("Failed to write image for key '%s'", key.getKey()), e);
    }

    private static IllegalArgumentException noImageRegistered(final Key key) {
        return new IllegalArgumentException(String.format("No image registered for key '%s'", key.getKey()));
    }

    private static IllegalArgumentException imageAlreadyRegistered(final Key key) {
        throw new IllegalArgumentException(String.format("Image already registered for key '%s'", key.getKey()));
    }
}
