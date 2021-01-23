package net.pl3x.map.plugin.command.commands;

import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.CommandManager;
import net.pl3x.map.plugin.command.Pl3xMapCommand;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.TimeUnit;

public final class ConfirmCommand extends Pl3xMapCommand {

    private final CommandConfirmationManager<CommandSender> confirmationManager = new CommandConfirmationManager<>(
            15L,
            TimeUnit.SECONDS,
            context -> Lang.send(
                    context.getCommandContext().getSender(),
                    Lang.CONFIRMATION_REQUIRED_MESSAGE,
                    Template.of("command", Config.MAIN_COMMAND_LABEL)
            ),
            sender -> Lang.send(sender, Lang.NO_PENDING_COMMANDS_MESSAGE)
    );

    public ConfirmCommand(final @NonNull Pl3xMapPlugin plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        this.confirmationManager.registerConfirmationProcessor(this.commandManager);

        this.commandManager.registerSubcommand(builder ->
                builder.literal("confirm")
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.get().parse(Lang.CONFIRM_COMMAND_DESCRIPTION))
                        .handler(this.confirmationManager.createConfirmationExecutionHandler()));
    }
}
