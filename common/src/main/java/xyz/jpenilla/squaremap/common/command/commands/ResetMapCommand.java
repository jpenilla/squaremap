package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.command.argument.LevelArgument;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.data.DirectoryProvider;
import xyz.jpenilla.squaremap.common.util.Components;
import xyz.jpenilla.squaremap.common.util.FileUtil;

@DefaultQualifier(NonNull.class)
public final class ResetMapCommand extends SquaremapCommand {
    private final DirectoryProvider directoryProvider;

    @Inject
    private ResetMapCommand(
        final Commands commands,
        final DirectoryProvider directoryProvider
    ) {
        super(commands);
        this.directoryProvider = directoryProvider;
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("resetmap")
                .argument(LevelArgument.of("world"))
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.RESETMAP_COMMAND_DESCRIPTION))
                .meta(CommandConfirmationManager.META_CONFIRMATION_REQUIRED, true)
                .permission("squaremap.command.resetmap")
                .handler(this::executeResetMap));
    }

    private void executeResetMap(final CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final ServerLevel world = context.get("world");
        final Path worldTilesDir = this.directoryProvider.getAndCreateTilesDirectory(world);
        try {
            FileUtil.deleteContentsRecursively(worldTilesDir);
        } catch (final IOException ex) {
            throw new RuntimeException("Could not reset map for level '" + world.dimension().location() + "'", ex);
        }
        Lang.send(sender, Lang.SUCCESSFULLY_RESET_MAP, Placeholder.unparsed("world", world.dimension().location().toString()));
    }
}
