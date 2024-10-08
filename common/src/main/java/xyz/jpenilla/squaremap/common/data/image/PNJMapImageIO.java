package xyz.jpenilla.squaremap.common.data.image;

import io.github.xfacthd.pnj.api.PNJ;
import io.github.xfacthd.pnj.api.data.Image;
import io.github.xfacthd.pnj.api.define.ColorFormat;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class PNJMapImageIO implements MapImageIO<PNJMapImageIO.PNJMapImage> {

    @Override
    public void save(final PNJMapImage image, final OutputStream out) throws IOException {
        PNJ.encode(out, image.image());
    }

    @Override
    public PNJMapImage load(final Path input) throws IOException {
        return new PNJMapImage(PNJ.decode(input));
    }

    @Override
    public PNJMapImage newImage() {
        return new PNJMapImage(
            new Image(
                MapImage.SIZE,
                MapImage.SIZE,
                ColorFormat.RGB_ALPHA,
                8,
                new byte[MapImage.SIZE * MapImage.SIZE * 4]
            )
        );
    }

    public record PNJMapImage(Image image) implements IOMapImage {
        @Override
        public void setPixel(int x, int y, int color) {
            this.image.setPixel(x, y, color, true);
        }
    }
}
