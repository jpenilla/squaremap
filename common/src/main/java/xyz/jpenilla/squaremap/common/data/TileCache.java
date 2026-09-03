package xyz.jpenilla.squaremap.common.data;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.FileUtil;

/**
 * Owns the tile image files of a single world. Tiles below the most detailed zoom level are
 * shared by several regions, so they are held in memory and only encoded once the render has
 * moved away from them, instead of being decoded and re-encoded for every region that draws
 * into them.
 */
@DefaultQualifier(NonNull.class)
public final class TileCache {
    private static final int MAX_ENTRIES = Integer.getInteger("squaremap.tileCacheSize", 16);

    private final Path directory;
    private final TileUpdates tileUpdates;
    private final Map<TileCoordinate, Entry> entries = new LinkedHashMap<>();
    private final Set<Integer> createdDirectories = new HashSet<>();

    public TileCache(final Path directory, final TileUpdates tileUpdates) {
        this.directory = directory;
        this.tileUpdates = tileUpdates;
    }

    /**
     * Draws into the tile at the given coordinate.
     *
     * @param coordinate tile to draw into
     * @param retain     whether the tile is shared with other regions and should be held in
     *                   memory until it is evicted or flushed, instead of written immediately
     * @param painter    receives the tile image
     */
    public synchronized void draw(final TileCoordinate coordinate, final boolean retain, final Consumer<BufferedImage> painter) {
        if (!retain) {
            final BufferedImage image = this.read(coordinate);
            painter.accept(image);
            this.write(coordinate, image);
            return;
        }

        final Entry entry = this.entry(coordinate);
        painter.accept(entry.image);
        entry.dirty = true;
    }

    /**
     * Writes out every tile that has been drawn into since the last flush. Tiles are kept in
     * memory so that a render still working in the same area doesn't have to decode them
     * again; a flush that finds nothing to write means the render has moved on, and drops
     * them so that an idle world holds no tile images.
     */
    public synchronized void flush() {
        boolean wrote = false;
        for (final Map.Entry<TileCoordinate, Entry> entry : this.entries.entrySet()) {
            wrote |= this.writeIfDirty(entry.getKey(), entry.getValue());
        }
        if (!wrote) {
            this.entries.clear();
        }
    }

    private Entry entry(final TileCoordinate coordinate) {
        // remove before putting so the map stays ordered from least to most recently used
        final @Nullable Entry existing = this.entries.remove(coordinate);
        if (existing != null) {
            this.entries.put(coordinate, existing);
            return existing;
        }

        final Entry entry = new Entry(this.read(coordinate));
        this.entries.put(coordinate, entry);

        final Iterator<Map.Entry<TileCoordinate, Entry>> it = this.entries.entrySet().iterator();
        while (this.entries.size() > MAX_ENTRIES && it.hasNext()) {
            final Map.Entry<TileCoordinate, Entry> eldest = it.next();
            it.remove();
            this.writeIfDirty(eldest.getKey(), eldest.getValue());
        }

        return entry;
    }

    private boolean writeIfDirty(final TileCoordinate coordinate, final Entry entry) {
        if (!entry.dirty) {
            return false;
        }
        this.write(coordinate, entry.image);
        entry.dirty = false;
        return true;
    }

    private BufferedImage read(final TileCoordinate coordinate) {
        final Path file = this.tileFile(coordinate);

        if (!Files.isRegularFile(file)) {
            return newBufferedImage();
        }

        try {
            final @Nullable BufferedImage read = ImageIO.read(file.toFile());
            if (read == null) {
                throw new IOException("Failed to read image file " + file.toAbsolutePath() + ", ImageIO.read(File) result is null. This means no " +
                    "supported image format was able to read it. The image file may have been malformed or corrupted, it will be overwritten.");
            }
            return read;
        } catch (final IOException ex) {
            try {
                Files.deleteIfExists(file);
            } catch (final IOException ex0) {
                ex.addSuppressed(ex0);
            }
            Logging.logger().error(xz(Messages.LOG_COULD_NOT_READ_REGION, coordinate), ex);
            return newBufferedImage();
        }
    }

    private void write(final TileCoordinate coordinate, final BufferedImage image) {
        final Path out = this.tileFile(coordinate);
        try {
            FileUtil.atomicWrite(out, tmp -> {
                try (final OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(tmp))) {
                    encode(image, outputStream);
                }
            });
        } catch (final IOException ex) {
            Logging.logger().error(xz(Messages.LOG_COULD_NOT_SAVE_REGION, coordinate), ex);
            return;
        }
        this.tileUpdates.record(coordinate);
    }

    private static void encode(final BufferedImage image, final OutputStream out) throws IOException {
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        try (final ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(imageOutputStream);
            final ImageWriteParam param = writer.getDefaultWriteParam();
            if (Config.COMPRESS_IMAGES && param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                if (param.getCompressionType() == null) {
                    param.setCompressionType(param.getCompressionTypes()[0]);
                }
                param.setCompressionQuality(Config.COMPRESSION_RATIO);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        }
    }

    private Path tileFile(final TileCoordinate coordinate) {
        final Path dir = this.directory.resolve(Integer.toString(coordinate.zoom()));
        if (this.createdDirectories.add(coordinate.zoom()) && !Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (final IOException e) {
                throw new RuntimeException(Logging.replace(Messages.LOG_COULD_NOT_CREATE_DIR, "path", dir.toAbsolutePath()), e);
            }
        }
        return dir.resolve(coordinate.x() + "_" + coordinate.z() + ".png");
    }

    private static BufferedImage newBufferedImage() {
        return new BufferedImage(Image.SIZE, Image.SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static String xz(final String message, final TileCoordinate coordinate) {
        return Logging.replace(message, "x", coordinate.x(), "z", coordinate.z());
    }

    private static final class Entry {
        private final BufferedImage image;
        private boolean dirty = false;

        private Entry(final BufferedImage image) {
            this.image = image;
        }
    }
}
