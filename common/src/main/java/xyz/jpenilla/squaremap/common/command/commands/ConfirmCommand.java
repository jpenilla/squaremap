package xyz.jpenilla.squaremap.common.command.commands;

import com.google.inject.Inject;
import java.time.Duration;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.processors.cache.SimpleCache;
import org.incendo.cloud.processors.confirmation.ConfirmationConfiguration;
import org.incendo.cloud.processors.confirmation.ConfirmationManager;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.Components;

import static net.kyori.adventure.text.Component.text;
import static org.incendo.cloud.minecraft.extras.RichDescription.richDescription;

@DefaultQualifier(NonNull.class)
public final class ConfirmCommand extends SquaremapCommand {
    private final ConfirmationManager<Commander> confirmationManager = ConfirmationManager.of(
        ConfirmationConfiguration.<Commander>builder()
            .cache(SimpleCache.of())
            .noPendingCommandNotifier(sender -> sender.sendMessage(Messages.NO_PENDING_COMMANDS_MESSAGE))
            .confirmationRequiredNotifier((sender, ctx) -> sender.sendMessage(confirmationRequiredMessage()))
            .expiration(Duration.ofSeconds(15))
            .build()
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
        this.commands.commandManager().registerCommandPostProcessor(
            this.confirmationManager.createPostprocessor()
        );

        this.commands.registerSubcommand(builder ->
            builder.literal("confirm")
                .commandDescription(richDescription(Messages.CONFIRM_COMMAND_DESCRIPTION))
                .handler(this.confirmationManager.createExecutionHandler()));
    }
}
