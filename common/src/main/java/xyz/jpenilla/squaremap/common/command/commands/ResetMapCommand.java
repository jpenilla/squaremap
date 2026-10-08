package xyz.jpenilla.squaremap.common.command.commands;

import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.processors.confirmation.ConfirmationManager;
import xyz.jpenilla.squaremap.common.SquaremapDirectories;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.FileUtil;
import xyz.jpenilla.squaremap.common.util.text.Components;
import xyz.jpenilla.squaremap.common.web.WebJsonStore;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;
import xyz.jpenilla.squaremap.common.world.WorldManagerImpl;

import static org.incendo.cloud.minecraft.extras.RichDescription.richDescription;
import static xyz.jpenilla.squaremap.common.command.argument.parser.LevelParser.levelParser;

@DefaultQualifier(NonNull.class)
public final class ResetMapCommand extends SquaremapCommand {
    private final SquaremapDirectories directories;
    private final WorldManagerImpl worldManager;
    private final WebJsonStore jsonStore;

    @Inject
    private ResetMapCommand(
        final Commands commands,
        final SquaremapDirectories directories,
        final WorldManagerImpl worldManager,
        final WebJsonStore jsonStore
    ) {
        super(commands);
        this.directories = directories;
        this.worldManager = worldManager;
        this.jsonStore = jsonStore;
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("resetmap")
                .required("world", levelParser())
                .commandDescription(richDescription(Messages.RESETMAP_COMMAND_DESCRIPTION))
                .meta(ConfirmationManager.META_CONFIRMATION_REQUIRED, true)
                .permission("squaremap.command.resetmap")
                .handler(this::executeResetMap));
    }

    private void executeResetMap(final CommandContext<Commander> context) {
        final Commander sender = context.sender();
        final ServerLevel world = context.get("world");
        final Optional<MapWorldInternal> mapWorld = this.worldManager.getWorldIfEnabled(world);
        if (mapWorld.isPresent() && mapWorld.get().renderScheduler().isRendering()) {
            sender.sendMessage(Messages.RENDER_IN_PROGRESS.withPlaceholders(Components.worldPlaceholder(world)));
            return;
        }
        final Path tilesDirectory = this.directories.getAndCreateTilesDirectory(world);
        // Shut the map world down like an unload, so no render or queued save writes into the cleared tiles.
        this.worldManager.worldUnloaded(world);
        try {
            FileUtil.deleteContentsRecursively(tilesDirectory);
        } catch (final IOException ex) {
            throw new RuntimeException("Could not reset map for level '" + world.dimension().identifier() + "'", ex);
        } finally {
            // Cached documents may describe deleted files, so drop them to have the next update rewrite them.
            this.jsonStore.clear(tilesDirectory);
            if (mapWorld.isPresent()) {
                this.worldManager.initWorld(world);
            }
        }
        this.worldManager.getWorldIfEnabled(world).ifPresent(MapWorldInternal::didReset);
        sender.sendMessage(Messages.SUCCESSFULLY_RESET_MAP.withPlaceholders(Components.worldPlaceholder(world)));
    }
}
