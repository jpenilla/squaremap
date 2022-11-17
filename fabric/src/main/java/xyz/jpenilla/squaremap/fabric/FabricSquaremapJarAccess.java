package xyz.jpenilla.squaremap.fabric;

import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
    public void useJar(final CheckedConsumer<Path, IOException> consumer) throws IOException {
        final List<Path> roots = this.modContainer.getRootPaths();
        if (roots.size() != 1) {
            throw new IllegalStateException("Expected one root!");
        }
        consumer.accept(roots.get(0));
    }
}
