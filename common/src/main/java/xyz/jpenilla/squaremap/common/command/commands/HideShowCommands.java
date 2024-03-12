package xyz.jpenilla.squaremap.common.command.commands;

import com.google.inject.Inject;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.context.CommandContext;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.Components;
import xyz.jpenilla.squaremap.common.util.EntityScheduler;

import static org.incendo.cloud.minecraft.extras.RichDescription.richDescription;

@DefaultQualifier(NonNull.class)
public final class HideShowCommands extends SquaremapCommand {
    private final PlatformCommands platformCommands;
    private final EntityScheduler entityScheduler;

    @Inject
    private HideShowCommands(
        final Commands commands,
        final PlatformCommands platformCommands,
        final EntityScheduler entityScheduler
    ) {
        super(commands);
        this.platformCommands = platformCommands;
        this.entityScheduler = entityScheduler;
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("hide")
                .commandDescription(richDescription(Messages.HIDE_COMMAND_DESCRIPTION))
                .permission("squaremap.command.hide")
                .handler(this::executeHide));
        this.commands.registerSubcommand(builder ->
            builder.literal("hide")
                .required("player", this.platformCommands.singlePlayerSelectorParser(), richDescription(Messages.OPTIONAL_PLAYER_ARGUMENT_DESCRIPTION))
                .commandDescription(richDescription(Messages.HIDE_COMMAND_DESCRIPTION))
                .permission("squaremap.command.hide.others")
                .handler(this::executeHide));

        this.commands.registerSubcommand(builder ->
            builder.literal("show")
                .commandDescription(richDescription(Messages.SHOW_COMMAND_DESCRIPTION))
                .permission("squaremap.command.show")
                .handler(this::executeShow));
        this.commands.registerSubcommand(builder ->
            builder.literal("show")
                .required("player", this.platformCommands.singlePlayerSelectorParser(), richDescription(Messages.OPTIONAL_PLAYER_ARGUMENT_DESCRIPTION))
                .commandDescription(richDescription(Messages.SHOW_COMMAND_DESCRIPTION))
                .permission("squaremap.command.show.others")
                .handler(this::executeShow));
    }

    // we need to schedule command feedback due to command blocks :D

    private void executeHide(final CommandContext<Commander> context) {
        final ServerPlayer target = this.platformCommands.extractPlayer("player", context)
            .orElseThrow(() -> CommandCompleted.withMessage(Messages.CONSOLE_MUST_SPECIFY_PLAYER));
        final Commander sender = context.sender();

        this.entityScheduler.scheduleFor(target, () -> {
            if (context.get(Commands.PLAYER_MANAGER).hidden(target.getUUID())) {
                this.entityScheduler.scheduleFor(sender, () -> sender.sendMessage(Messages.PLAYER_ALREADY_HIDDEN.withPlaceholders(Components.playerPlaceholder(target))));
                return;
            }

            context.get(Commands.PLAYER_MANAGER).hide(target.getUUID(), true);
            this.entityScheduler.scheduleFor(sender, () -> sender.sendMessage(Messages.PLAYER_HIDDEN.withPlaceholders(Components.playerPlaceholder(target))));
        });
    }

    private void executeShow(final CommandContext<Commander> context) {
        final ServerPlayer target = this.platformCommands.extractPlayer("player", context)
            .orElseThrow(() -> CommandCompleted.withMessage(Messages.CONSOLE_MUST_SPECIFY_PLAYER));
        final Commander sender = context.sender();

        this.entityScheduler.scheduleFor(target, () -> {
            if (!context.get(Commands.PLAYER_MANAGER).hidden(target.getUUID())) {
                this.entityScheduler.scheduleFor(sender, () -> sender.sendMessage(Messages.PLAYER_NOT_HIDDEN.withPlaceholders(Components.playerPlaceholder(target))));
                return;
            }

            context.get(Commands.PLAYER_MANAGER).show(target.getUUID(), true);
            this.entityScheduler.scheduleFor(sender, () -> sender.sendMessage(Messages.PLAYER_SHOWN.withPlaceholders(Components.playerPlaceholder(target))));
        });
    }
}
