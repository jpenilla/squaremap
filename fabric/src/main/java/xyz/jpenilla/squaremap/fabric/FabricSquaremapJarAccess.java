package xyz.jpenilla.squaremap.fabric;

import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import net.fabricmc.loader.api.ModContainer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.CheckedConsumer;
import xyz.jpenilla.squaremap.common.util.SquaremapJarAccess;

@DefaultQualifier(NonNull.class)
final class FabricSquaremapJarAccess implements SquaremapJarAccess {
    private final ModContainer modContainer;

    @Inject
    private FabricSquaremapJarAccess(final ModContainer modContainer) {
        this.modContainer = modContainer;
    }

    @Override
    public void usePath(final String path, final CheckedConsumer<Path, IOException> consumer) throws IOException {
        // Searches every root: in development, common's output is a separate root of the mod.
        consumer.accept(this.modContainer.findPath(path).orElseThrow(() -> new NoSuchFileException(path)));
    }
}
