package xyz.jpenilla.squaremap.paper.inject.module;

import com.google.inject.AbstractModule;
import io.papermc.paper.text.PaperComponents;
import java.nio.file.Path;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.bukkit.Server;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.inject.annotation.DataDirectory;
import xyz.jpenilla.squaremap.common.util.ChunkSnapshotProvider;
import xyz.jpenilla.squaremap.common.util.RegionFileDirectoryResolver;
import xyz.jpenilla.squaremap.paper.PaperServerAccess;
import xyz.jpenilla.squaremap.paper.SquaremapPaper;
import xyz.jpenilla.squaremap.paper.command.PaperCommands;
import xyz.jpenilla.squaremap.paper.util.PaperChunkSnapshotProvider;
import xyz.jpenilla.squaremap.paper.util.PaperRegionFileDirectoryResolver;
import xyz.jpenilla.squaremap.paper.util.WorldNameToKeyMigration;

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

        this.bind(Server.class)
            .toInstance(this.plugin.getServer());

        this.bind(ServerAccess.class)
            .to(PaperServerAccess.class);

        this.bind(Path.class)
            .annotatedWith(DataDirectory.class)
            .toInstance(this.plugin.getDataFolder().toPath());

        this.bind(RegionFileDirectoryResolver.class)
            .to(PaperRegionFileDirectoryResolver.class);

        this.bind(PlatformCommands.class)
            .to(PaperCommands.class);

        this.bind(ChunkSnapshotProvider.class)
            .to(PaperChunkSnapshotProvider.class);

        this.bind(ComponentFlattener.class)
            .toInstance(PaperComponents.flattener());

        this.requestStaticInjection(WorldNameToKeyMigration.class);
    }
}
