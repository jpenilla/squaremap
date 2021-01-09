package net.pl3x.map.command;

import cloud.commandframework.Command;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import com.google.common.collect.ImmutableSet;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.command.commands.CancelRenderCommand;
import net.pl3x.map.command.commands.FullRenderCommand;
import net.pl3x.map.command.commands.RadiusRenderCommand;
import net.pl3x.map.command.commands.ReloadCommand;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.UnaryOperator;

public final class CommandManager extends PaperCommandManager<CommandSender> {

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

        ImmutableSet.of(
                new ReloadCommand(plugin, this),
                new FullRenderCommand(plugin, this),
                new CancelRenderCommand(plugin, this),
                new RadiusRenderCommand(plugin, this)
        ).forEach(Pl3xMapCommand::register);

    }

    @SafeVarargs
    public final void commands(final @NonNull Command<CommandSender> @NonNull ... commands) {
        for (final Command<CommandSender> command : commands) {
            this.command(command);
        }
    }

}
