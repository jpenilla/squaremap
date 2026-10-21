package xyz.jpenilla.squaremap.common.render.output;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
import xyz.jpenilla.squaremap.common.coordinate.TileCoordinate;
import xyz.jpenilla.squaremap.common.util.FileUtil;

/**
 * Reads and writes a world's tile images.
 *
 * @param directory the world's tiles directory
 */
@DefaultQualifier(NonNull.class)
record TileImages(Path directory) {
    static final int SIZE = 512;

    Path path(final TileCoordinate tile) {
        return this.directory.resolve(Integer.toString(tile.zoom())).resolve(tile.x() + "_" + tile.z() + ".png");
    }

    /**
     * Reads a tile, or creates a blank one if it doesn't exist or can't be read.
     *
     * @param tile the tile
     * @return the tile image
     */
    BufferedImage read(final TileCoordinate tile) {
        final Path file = this.path(tile);
        if (!Files.isRegularFile(file)) {
            return blank();
        }

        try {
            final @Nullable BufferedImage read = ImageIO.read(file.toFile());
            if (read == null) {
                throw new IOException("No supported image reader could read the file");
            }
            return read;
        } catch (final IOException ex) {
            try {
                Files.deleteIfExists(file);
            } catch (final IOException ex0) {
                ex.addSuppressed(ex0);
            }
            Logging.error(Messages.LOG_COULD_NOT_READ_IMAGE, ex, "path", file);
            return blank();
        }
    }

    /**
     * Atomically replaces a tile's file.
     *
     * @param tile  the tile
     * @param image the tile image
     * @throws IOException if the file can't be written
     */
    void write(final TileCoordinate tile, final BufferedImage image) throws IOException {
        final Path file = this.path(tile);
        Files.createDirectories(file.getParent());
        FileUtil.atomicWrite(file, tmp -> {
            try (final OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(tmp))) {
                write(image, outputStream);
            }
        });
    }

    private static void write(final BufferedImage image, final OutputStream out) throws IOException {
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

    private static BufferedImage blank() {
        return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }
}
