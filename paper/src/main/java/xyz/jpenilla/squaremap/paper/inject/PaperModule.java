package xyz.jpenilla.squaremap.paper.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.papermc.paper.text.PaperComponents;
import java.nio.file.Path;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.inject.annotation.DataDirectory;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.paper.SquaremapPaper;
import xyz.jpenilla.squaremap.paper.command.PaperCommands;
import xyz.jpenilla.squaremap.paper.util.PaperChunkSnapshotProvider;

@DefaultQualifier(NonNull.class)
public final class PaperModule extends AbstractModule {
    private final SquaremapPaper plugin;

    public PaperModule(final SquaremapPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        this.bind(SquaremapPaper.class)
            .toInstance(this.plugin);

        this.bind(Path.class)
            .annotatedWith(DataDirectory.class)
            .toInstance(this.plugin.getDataFolder().toPath());

        this.bind(PlatformCommands.class)
            .to(PaperCommands.class);

        this.bind(ChunkSnapshotProvider.class)
            .to(PaperChunkSnapshotProvider.class);
    }

    @Provides
    public ComponentFlattener componentFlattener() {
        return PaperComponents.flattener();
    }
}
