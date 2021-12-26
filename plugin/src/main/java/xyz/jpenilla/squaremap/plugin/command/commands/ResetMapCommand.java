package xyz.jpenilla.squaremap.plugin.command.commands;

import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import java.io.IOException;
import java.nio.file.Path;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.command.Commands;
import xyz.jpenilla.squaremap.plugin.command.SquaremapCommand;
import xyz.jpenilla.squaremap.plugin.config.Lang;
import xyz.jpenilla.squaremap.plugin.util.FileUtil;

public final class ResetMapCommand extends SquaremapCommand {

    public ResetMapCommand(final @NonNull SquaremapPlugin plugin, final @NonNull Commands commands) {
        super(plugin, commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
                builder.literal("resetmap")
                        .argument(WorldArgument.of("world"))
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.RESETMAP_COMMAND_DESCRIPTION))
                        .meta(CommandConfirmationManager.META_CONFIRMATION_REQUIRED, true)
                        .permission("squaremap.command.resetmap")
                        .handler(this::executeResetMap));
    }

    private void executeResetMap(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final World world = context.get("world");
        final Path worldTilesDir = FileUtil.getWorldFolder(world);
        try {
            FileUtil.deleteSubdirectories(worldTilesDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not reset map", e);
        }
        Lang.send(sender, Lang.SUCCESSFULLY_RESET_MAP, Template.template("world", world.getName()));
    }
}
