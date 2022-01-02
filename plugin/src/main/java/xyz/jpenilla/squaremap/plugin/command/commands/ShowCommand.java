package xyz.jpenilla.squaremap.plugin.command.commands;

import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.command.Commands;
import xyz.jpenilla.squaremap.plugin.command.SquaremapCommand;
import xyz.jpenilla.squaremap.plugin.util.CommandUtil;

public final class ShowCommand extends SquaremapCommand {

    public ShowCommand(final @NonNull SquaremapPlugin plugin, final @NonNull Commands commands) {
        super(plugin, commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
                builder.literal("show")
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.SHOW_COMMAND_DESCRIPTION))
                        .permission("squaremap.command.show")
                        .handler(this::executeShow));
        this.commands.registerSubcommand(builder ->
                builder.literal("show")
                        .argument(SinglePlayerSelectorArgument.of("player"), CommandUtil.description(Lang.OPTIONAL_PLAYER_ARGUMENT_DESCRIPTION))
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.SHOW_COMMAND_DESCRIPTION))
                        .permission("squaremap.command.show.others")
                        .handler(this::executeShow));
    }

    private void executeShow(final @NonNull CommandContext<CommandSender> context) {
        final Player target = CommandUtil.resolvePlayer(context);
        final CommandSender sender = context.getSender();
        if (!this.plugin.playerManager().hidden(target)) {
            Lang.send(sender, Lang.PLAYER_NOT_HIDDEN, Template.template("player", target.getName()));
            return;
        }

        this.plugin.playerManager().show(target, true);
        Lang.send(sender, Lang.PLAYER_SHOWN, Template.template("player", target.getName()));
    }

}
