package xyz.jpenilla.squaremap.common.data.image;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface MapImageIO<I extends MapImageIO.IOMapImage> {
    void save(I image, OutputStream out) throws IOException;

    I load(Path input) throws IOException;

    I newImage();

    interface IOMapImage {
        void setPixel(int x, int y, int color);
    }
}
