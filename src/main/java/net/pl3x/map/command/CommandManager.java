package net.pl3x.map.command;

import cloud.commandframework.Command;
import cloud.commandframework.CommandHelpHandler;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.exceptions.InvalidCommandSenderException;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.google.common.collect.ImmutableList;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.command.commands.CancelRenderCommand;
import net.pl3x.map.command.commands.ConfirmCommand;
import net.pl3x.map.command.commands.FullRenderCommand;
import net.pl3x.map.command.commands.RadiusRenderCommand;
import net.pl3x.map.command.commands.ReloadCommand;
import net.pl3x.map.command.commands.ResetMapCommand;
import net.pl3x.map.configuration.Lang;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.UnaryOperator;

public final class CommandManager extends PaperCommandManager<CommandSender> {

    public static final CommandMeta.Key<String> INVALID_SENDER_ALTERNATE_COMMAND = CommandMeta.Key.of(String.class, "pl3xmap:invalid_sender_alternate_command");

    public CommandManager(final @NonNull Pl3xMap plugin) throws Exception {

        super(
                plugin,
                CommandExecutionCoordinator.simpleCoordinator(),
                UnaryOperator.identity(),
                UnaryOperator.identity()
        );

        if (this.queryCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            this.registerBrigadier();
            final CloudBrigadierManager<?, ?> brigManager = this.brigadierManager();
            if (brigManager != null) {
                brigManager.setNativeNumberSuggestions(false);
            }
        }

        if (this.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            this.registerAsynchronousCompletions();
        }

        this.registerExceptionHandler(InvalidCommandSenderException.class, (sender, ex) -> {
            final String altCommand = ex.getCommand().getCommandMeta().getOrDefault(INVALID_SENDER_ALTERNATE_COMMAND, "");
            if (!altCommand.isEmpty()) {
                CommandHelpHandler.VerboseHelpTopic<CommandSender> topic = ((CommandHelpHandler.VerboseHelpTopic<CommandSender>) this.getCommandHelpHandler().queryHelp(altCommand));
                final String alternateCommandSyntax = this.getCommandSyntaxFormatter().apply(topic.getCommand().getArguments(), null);
                Lang.send(sender, Lang.INVALID_COMMAND_SYNTAX.replace("{syntax}", alternateCommandSyntax));
                return;
            }
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        });

        ImmutableList.of(
                new ReloadCommand(plugin, this),
                new ConfirmCommand(plugin, this),
                new FullRenderCommand(plugin, this),
                new CancelRenderCommand(plugin, this),
                new RadiusRenderCommand(plugin, this),
                new ResetMapCommand(plugin, this)
        ).forEach(Pl3xMapCommand::register);

    }

    public void registerSubcommand(UnaryOperator<Command.Builder<CommandSender>> builderModifier) {
        this.command(builderModifier.apply(this.rootBuilder()));
    }

    private Command.@NonNull Builder<CommandSender> rootBuilder() {
        return this.commandBuilder("pl3xmap", "map");
    }

}
