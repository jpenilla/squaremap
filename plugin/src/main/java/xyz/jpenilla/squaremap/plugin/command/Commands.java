package xyz.jpenilla.squaremap.plugin.command;

import cloud.commandframework.Command;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.SimpleCloudKey;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.paper.PaperCommandManager;
import io.leangen.geantyref.TypeToken;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.command.commands.CancelRenderCommand;
import xyz.jpenilla.squaremap.plugin.command.commands.ConfirmCommand;
import xyz.jpenilla.squaremap.plugin.command.commands.FullRenderCommand;
import xyz.jpenilla.squaremap.plugin.command.commands.HelpCommand;
import xyz.jpenilla.squaremap.plugin.command.commands.HideCommand;
import xyz.jpenilla.squaremap.plugin.command.commands.PauseRenderCommand;
import xyz.jpenilla.squaremap.plugin.command.commands.ProgressLoggingCommand;
import xyz.jpenilla.squaremap.plugin.command.commands.RadiusRenderCommand;
import xyz.jpenilla.squaremap.plugin.command.commands.ReloadCommand;
import xyz.jpenilla.squaremap.plugin.command.commands.ResetMapCommand;
import xyz.jpenilla.squaremap.plugin.command.commands.ShowCommand;
import xyz.jpenilla.squaremap.plugin.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.plugin.command.exception.ConsoleMustProvideWorldException;
import xyz.jpenilla.squaremap.plugin.configuration.Config;
import xyz.jpenilla.squaremap.plugin.configuration.Lang;

@DefaultQualifier(NonNull.class)
public final class Commands {
    public static final CloudKey<SquaremapPlugin> PLUGIN_INSTANCE_KEY = SimpleCloudKey.of("plugin-instance", TypeToken.get(SquaremapPlugin.class));

    private final PaperCommandManager<CommandSender> commandManager;

    public Commands(final SquaremapPlugin plugin) {
        try {
            this.commandManager = PaperCommandManager.createNative(
                plugin,
                CommandExecutionCoordinator.simpleCoordinator()
            );
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to initialize command manager", ex);
        }

        this.commandManager.registerCommandPreProcessor(ctx -> ctx.getCommandContext().store(PLUGIN_INSTANCE_KEY, plugin));

        if (this.commandManager.queryCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            this.commandManager.registerBrigadier();
            final @Nullable CloudBrigadierManager<?, ?> brigManager = this.commandManager.brigadierManager();
            if (brigManager != null) {
                brigManager.setNativeNumberSuggestions(false);
            }
        }

        if (this.commandManager.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            this.commandManager.registerAsynchronousCompletions();
        }

        this.registerExceptionHandlers();

        List.of(
            new HelpCommand(plugin, this),
            new ReloadCommand(plugin, this),
            new ConfirmCommand(plugin, this),
            new FullRenderCommand(plugin, this),
            new CancelRenderCommand(plugin, this),
            new RadiusRenderCommand(plugin, this),
            new PauseRenderCommand(plugin, this),
            new ResetMapCommand(plugin, this),
            new HideCommand(plugin, this),
            new ShowCommand(plugin, this),
            new ProgressLoggingCommand(plugin, this)
        ).forEach(SquaremapCommand::register);

    }

    private void registerExceptionHandlers() {
        new MinecraftExceptionHandler<CommandSender>()
            .withDefaultHandlers()
            .withDecorator(component -> Component.text()
                .append(MiniMessage.miniMessage().parse(Lang.COMMAND_PREFIX)
                    .hoverEvent(MiniMessage.miniMessage().parse(Lang.CLICK_FOR_HELP))
                    .clickEvent(ClickEvent.runCommand(String.format("/%s help", Config.MAIN_COMMAND_LABEL))))
                .append(component)
                .build())
            .apply(this.commandManager, AudienceProvider.nativeAudience());

        final var minecraftExtrasDefaultHandler = Objects.requireNonNull(this.commandManager.getExceptionHandler(CommandExecutionException.class));
        this.commandManager.registerExceptionHandler(CommandExecutionException.class, (sender, exception) -> {
            final Throwable cause = exception.getCause();

            if (cause instanceof CommandCompleted commandCompleted) {
                final @Nullable Component message = commandCompleted.componentMessage();
                if (message != null) {
                    sender.sendMessage(message);
                }
                return;
            } else if (cause instanceof ConsoleMustProvideWorldException) {
                Lang.send(sender, Lang.CONSOLE_MUST_SPECIFY_WORLD);
                return;
            }

            minecraftExtrasDefaultHandler.accept(sender, exception);
        });
    }

    public void register(final Command.Builder<CommandSender> builder) {
        this.commandManager.command(builder);
    }

    public void registerSubcommand(UnaryOperator<Command.Builder<CommandSender>> builderModifier) {
        this.commandManager.command(builderModifier.apply(this.rootBuilder()));
    }

    public Command.Builder<CommandSender> rootBuilder() {
        return this.commandManager.commandBuilder(Config.MAIN_COMMAND_LABEL, Config.MAIN_COMMAND_ALIASES.toArray(String[]::new))
            /* MinecraftHelp uses the MinecraftExtrasMetaKeys.DESCRIPTION meta, this is just so we give Bukkit a description
             * for our commands in the Bukkit and EssentialsX '/help' command */
            .meta(CommandMeta.DESCRIPTION, String.format("squaremap command. '/%s help'", Config.MAIN_COMMAND_LABEL));
    }

    public PaperCommandManager<CommandSender> commandManager() {
        return this.commandManager;
    }

}
