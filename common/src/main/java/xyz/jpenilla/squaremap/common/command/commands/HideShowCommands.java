package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import cloud.commandframework.minecraft.extras.RichDescription;
import com.google.inject.Inject;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.Components;
import xyz.jpenilla.squaremap.common.util.EntityScheduler;

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
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.HIDE_COMMAND_DESCRIPTION.asComponent())
                .permission("squaremap.command.hide")
                .handler(this::executeHide));
        this.commands.registerSubcommand(builder ->
            builder.literal("hide")
                .argument(this.platformCommands.singlePlayerSelectorArgument("player"), RichDescription.of(Messages.OPTIONAL_PLAYER_ARGUMENT_DESCRIPTION))
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.HIDE_COMMAND_DESCRIPTION.asComponent())
                .permission("squaremap.command.hide.others")
                .handler(this::executeHide));

        this.commands.registerSubcommand(builder ->
            builder.literal("show")
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.SHOW_COMMAND_DESCRIPTION.asComponent())
                .permission("squaremap.command.show")
                .handler(this::executeShow));
        this.commands.registerSubcommand(builder ->
            builder.literal("show")
                .argument(this.platformCommands.singlePlayerSelectorArgument("player"), RichDescription.of(Messages.OPTIONAL_PLAYER_ARGUMENT_DESCRIPTION))
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.SHOW_COMMAND_DESCRIPTION.asComponent())
                .permission("squaremap.command.show.others")
                .handler(this::executeShow));
    }

    // we need to schedule command feedback due to command blocks :D

    private void executeHide(final CommandContext<Commander> context) {
        final ServerPlayer target = this.platformCommands.extractPlayer("player", context);
        final Commander sender = context.getSender();

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
        final ServerPlayer target = this.platformCommands.extractPlayer("player", context);
        final Commander sender = context.getSender();

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
