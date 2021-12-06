package net.pl3x.map.plugin.command.commands;

import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import java.io.IOException;
import java.nio.file.Path;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.CommandManager;
import net.pl3x.map.plugin.command.Pl3xMapCommand;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.util.FileUtil;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class ResetMapCommand extends Pl3xMapCommand {

    public ResetMapCommand(final @NonNull Pl3xMapPlugin plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        this.commandManager.registerSubcommand(builder ->
                builder.literal("resetmap")
                        .argument(WorldArgument.of("world"))
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.RESETMAP_COMMAND_DESCRIPTION))
                        .meta(CommandConfirmationManager.META_CONFIRMATION_REQUIRED, true)
                        .permission("pl3xmap.command.resetmap")
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
