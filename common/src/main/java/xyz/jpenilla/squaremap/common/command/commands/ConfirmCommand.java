package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.Components;

import static net.kyori.adventure.text.Component.text;

@DefaultQualifier(NonNull.class)
public final class ConfirmCommand extends SquaremapCommand {
    private final CommandConfirmationManager<Commander> confirmationManager = new CommandConfirmationManager<>(
        15L,
        TimeUnit.SECONDS,
        context -> context.getCommandContext().getSender().sendMessage(confirmationRequiredMessage()),
        sender -> sender.sendMessage(Messages.NO_PENDING_COMMANDS_MESSAGE)
    );

    private static ComponentLike confirmationRequiredMessage() {
        return text()
            .append(Messages.CONFIRMATION_REQUIRED_MESSAGE.withPlaceholders(Components.placeholder("command", Config.MAIN_COMMAND_LABEL)))
            .hoverEvent(Messages.CLICK_TO_CONFIRM.asComponent())
            .clickEvent(ClickEvent.runCommand('/' + Config.MAIN_COMMAND_LABEL + " confirm"));
    }

    @Inject
    private ConfirmCommand(final Commands commands) {
        super(commands);
    }

    @Override
    public void register() {
        this.confirmationManager.registerConfirmationProcessor(this.commands.commandManager());

        this.commands.registerSubcommand(builder ->
            builder.literal("confirm")
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.CONFIRM_COMMAND_DESCRIPTION.asComponent())
                .handler(this.confirmationManager.createConfirmationExecutionHandler()));
    }
}
