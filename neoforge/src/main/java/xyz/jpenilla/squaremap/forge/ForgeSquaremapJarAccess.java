package xyz.jpenilla.squaremap.forge;

import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import net.neoforged.fml.ModContainer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.util.CheckedConsumer;
import xyz.jpenilla.squaremap.common.util.FileUtil;
import xyz.jpenilla.squaremap.common.util.SquaremapJarAccess;

@DefaultQualifier(NonNull.class)
final class ForgeSquaremapJarAccess implements SquaremapJarAccess {
    private final ModContainer modContainer;

    @Inject
    private ForgeSquaremapJarAccess(final ModContainer modContainer) {
        this.modContainer = modContainer;
    }

    @Override
    public void useJar(final CheckedConsumer<Path, IOException> consumer) throws IOException {
        FileUtil.openJar(this.modContainer.getModInfo().getOwningFile().getFile().getFilePath(), fs -> consumer.accept(fs.getPath("/")));
    }
}
