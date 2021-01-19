package net.pl3x.map.plugin.api;

import net.pl3x.map.api.Key;
import net.pl3x.map.api.Pair;
import net.pl3x.map.api.Registry;
import net.pl3x.map.plugin.util.FileUtil;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class IconRegistry implements Registry<BufferedImage> {

    private final Map<Key, BufferedImage> images = new ConcurrentHashMap<>();
    private final Path directory;

    public IconRegistry() {
        this.directory = FileUtil.WEB_DIR.resolve("images/icon/registered/");
        try {
            FileUtil.deleteDirectory(this.directory);
            Files.createDirectories(this.directory);
        } catch (IOException e) {
            throw failedToCreateRegistry(e);
        }
    }

    @Override
    public void register(@NonNull Key key, @NonNull BufferedImage value) {
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
    public void unregister(@NonNull Key key) {
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
    public boolean hasEntry(@NonNull Key key) {
        return this.images.containsKey(key);
    }

    @Override
    public @NonNull BufferedImage get(@NonNull Key key) {
        final BufferedImage provider = this.images.get(key);
        if (provider == null) {
            throw noImageRegistered(key);
        }
        return provider;
    }

    @Override
    public @NonNull Iterable<Pair<Key, BufferedImage>> entries() {
        return this.images.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toUnmodifiableList());
    }

    private static @NonNull IllegalArgumentException failedToCreateRegistry(final @NonNull IOException e) {
        return new IllegalArgumentException("Failed to setup icon registry", e);
    }

    private static @NonNull IllegalArgumentException failedToDeleteImage(final @NonNull Key key, final @NonNull IOException e) {
        return new IllegalArgumentException(String.format("Failed to delete image for key '%s'", key.getKey()), e);
    }

    private static @NonNull IllegalArgumentException failedToWriteImage(final @NonNull Key key, final @NonNull IOException e) {
        return new IllegalArgumentException(String.format("Failed to write image for key '%s'", key.getKey()), e);
    }

    private static @NonNull IllegalArgumentException noImageRegistered(final @NonNull Key key) {
        return new IllegalArgumentException(String.format("No image registered for key '%s'", key.getKey()));
    }

    private static @NonNull IllegalArgumentException imageAlreadyRegistered(final @NonNull Key key) {
        throw new IllegalArgumentException(String.format("Image already registered for key '%s'", key.getKey()));
    }

}
