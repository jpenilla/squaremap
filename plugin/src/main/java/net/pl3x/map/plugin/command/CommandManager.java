package net.pl3x.map.plugin.command;

import cloud.commandframework.Command;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.paper.PaperCommandManager;
import com.google.common.collect.ImmutableList;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.commands.CancelRenderCommand;
import net.pl3x.map.plugin.command.commands.ConfirmCommand;
import net.pl3x.map.plugin.command.commands.FullRenderCommand;
import net.pl3x.map.plugin.command.commands.HelpCommand;
import net.pl3x.map.plugin.command.commands.RadiusRenderCommand;
import net.pl3x.map.plugin.command.commands.ReloadCommand;
import net.pl3x.map.plugin.command.commands.ResetMapCommand;
import net.pl3x.map.plugin.command.exception.CompletedSuccessfullyException;
import net.pl3x.map.plugin.command.exception.ConsoleMustProvideWorldException;
import net.pl3x.map.plugin.configuration.Lang;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.function.UnaryOperator;

public final class CommandManager extends PaperCommandManager<CommandSender> {

    public static final CommandMeta.Key<String> INVALID_SENDER_ALTERNATE_COMMAND = CommandMeta.Key.of(String.class, "pl3xmap:invalid_sender_alternate_command");

    public CommandManager(final @NonNull Pl3xMapPlugin plugin) throws Exception {

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

        new MinecraftExceptionHandler<CommandSender>()
                .withDefaultHandlers()
                .apply(this, plugin.audiences()::sender);

        final var minecraftExtrasDefaultHandler = Objects.requireNonNull(this.getExceptionHandler(CommandExecutionException.class));
        this.registerExceptionHandler(CommandExecutionException.class, (sender, exception) -> {
            final Throwable cause = exception.getCause();

            if (cause instanceof CompletedSuccessfullyException) {
                return;
            } else if (cause instanceof ConsoleMustProvideWorldException) {
                Lang.send(sender, Lang.WORLD_NOT_SPECIFIED);
                return;
            }

            minecraftExtrasDefaultHandler.accept(sender, exception);
        });

        ImmutableList.of(
                new HelpCommand(plugin, this),
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
