package net.pl3x.map.plugin.command.commands;

import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.CommandManager;
import net.pl3x.map.plugin.command.Pl3xMapCommand;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.util.CommandUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class HideCommand extends Pl3xMapCommand {

    public HideCommand(final @NonNull Pl3xMapPlugin plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        this.commandManager.registerSubcommand(builder ->
                builder.literal("hide")
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.get().parse(Lang.HIDE_COMMAND_DESCRIPTION))
                        .permission("pl3xmap.command.hide")
                        .handler(this::executeHide));
        this.commandManager.registerSubcommand(builder ->
                builder.literal("hide")
                        .argument(SinglePlayerSelectorArgument.optional("player"), CommandUtil.description(Lang.OPTIONAL_PLAYER_ARGUMENT_DESCRIPTION))
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.get().parse(Lang.HIDE_COMMAND_DESCRIPTION))
                        .permission("pl3xmap.command.hide.others")
                        .handler(this::executeHide));
    }

    private void executeHide(final @NonNull CommandContext<CommandSender> context) {
        final Player target = CommandUtil.resolvePlayer(context);
        final CommandSender sender = context.getSender();
        if (this.plugin.playerManager().hidden(target)) {
            Lang.send(sender, Lang.PLAYER_ALREADY_HIDDEN, Template.of("player", target.getName()));
            return;
        }

        this.plugin.playerManager().hide(target, true);
        Lang.send(sender, Lang.PLAYER_HIDDEN, Template.of("player", target.getName()));
    }

}
