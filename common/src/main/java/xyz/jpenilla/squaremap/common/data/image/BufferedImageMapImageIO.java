package xyz.jpenilla.squaremap.common.data.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.config.Config;

@DefaultQualifier(NonNull.class)
public final class BufferedImageMapImageIO implements MapImageIO<BufferedImageMapImageIO.BufferedImageMapImage> {
    @Override
    public void save(final BufferedImageMapImage image, final OutputStream out) throws IOException {
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
            writer.write(null, new IIOImage(image.image(), null, null), param);
        }
    }

    @Override
    public BufferedImageMapImage load(final Path input) throws IOException {
        final @Nullable BufferedImage read = ImageIO.read(input.toFile());
        if (read == null) {
            throw new IOException("Failed to read image file '" + input.toAbsolutePath() + "', ImageIO.read(File) result is null. This means no " +
                "supported image format was able to read it. The image file may have been malformed or corrupted, it will be overwritten.");
        }
        return new BufferedImageMapImage(read);
    }

    @Override
    public BufferedImageMapImage newImage() {
        return new BufferedImageMapImage(
            new BufferedImage(MapImage.SIZE, MapImage.SIZE, BufferedImage.TYPE_INT_ARGB)
        );
    }

    public record BufferedImageMapImage(BufferedImage image) implements MapImageIO.IOMapImage {
        @Override
        public void setPixel(final int x, final int y, final int color) {
            this.image.setRGB(x, y, color);
        }
    }
}
