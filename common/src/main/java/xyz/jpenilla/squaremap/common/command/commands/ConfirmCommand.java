package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.util.Components;

import static net.kyori.adventure.text.Component.text;

@DefaultQualifier(NonNull.class)
public final class ConfirmCommand extends SquaremapCommand {
    private final CommandConfirmationManager<Commander> confirmationManager = new CommandConfirmationManager<>(
        15L,
        TimeUnit.SECONDS,
        context -> context.getCommandContext().getSender().sendMessage(confirmationRequiredMessage()),
        sender -> Lang.send(sender, Lang.NO_PENDING_COMMANDS_MESSAGE)
    );

    private static ComponentLike confirmationRequiredMessage() {
        return text()
            .append(Components.miniMessage(Lang.CONFIRMATION_REQUIRED_MESSAGE, Placeholder.unparsed("command", Config.MAIN_COMMAND_LABEL)))
            .hoverEvent(Components.miniMessage(Lang.CLICK_TO_CONFIRM))
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
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.CONFIRM_COMMAND_DESCRIPTION))
                .handler(this.confirmationManager.createConfirmationExecutionHandler()));
    }
}
