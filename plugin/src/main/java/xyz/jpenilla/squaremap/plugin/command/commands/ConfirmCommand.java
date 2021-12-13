package xyz.jpenilla.squaremap.plugin.command.commands;

import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.command.Commands;
import xyz.jpenilla.squaremap.plugin.command.SquaremapCommand;
import xyz.jpenilla.squaremap.plugin.configuration.Config;
import xyz.jpenilla.squaremap.plugin.configuration.Lang;

import static net.kyori.adventure.text.Component.text;

public final class ConfirmCommand extends SquaremapCommand {

    private final CommandConfirmationManager<CommandSender> confirmationManager = new CommandConfirmationManager<>(
            15L,
            TimeUnit.SECONDS,
            context -> context.getCommandContext().getSender().sendMessage(confirmationRequiredMessage()),
            sender -> Lang.send(sender, Lang.NO_PENDING_COMMANDS_MESSAGE)
    );

    private static @NonNull ComponentLike confirmationRequiredMessage() {
        return text()
                .append(Lang.parse(Lang.CONFIRMATION_REQUIRED_MESSAGE, Template.template("command", Config.MAIN_COMMAND_LABEL)))
                .hoverEvent(Lang.parse(Lang.CLICK_TO_CONFIRM))
                .clickEvent(ClickEvent.runCommand('/' + Config.MAIN_COMMAND_LABEL + " confirm"));
    }

    public ConfirmCommand(final @NonNull SquaremapPlugin plugin, final @NonNull Commands commands) {
        super(plugin, commands);
    }

    @Override
    public void register() {
        this.confirmationManager.registerConfirmationProcessor(this.commands.commandManager());

        this.commands.registerSubcommand(builder ->
                builder.literal("confirm")
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.CONFIRM_COMMAND_DESCRIPTION))
                        .handler(this.confirmationManager.createConfirmationExecutionHandler()));
    }
}
