package net.pl3x.map.plugin.command;

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
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.commands.CancelRenderCommand;
import net.pl3x.map.plugin.command.commands.ConfirmCommand;
import net.pl3x.map.plugin.command.commands.FullRenderCommand;
import net.pl3x.map.plugin.command.commands.HelpCommand;
import net.pl3x.map.plugin.command.commands.HideCommand;
import net.pl3x.map.plugin.command.commands.PauseRenderCommand;
import net.pl3x.map.plugin.command.commands.RadiusRenderCommand;
import net.pl3x.map.plugin.command.commands.ReloadCommand;
import net.pl3x.map.plugin.command.commands.ResetMapCommand;
import net.pl3x.map.plugin.command.commands.ShowCommand;
import net.pl3x.map.plugin.command.exception.CommandCompleted;
import net.pl3x.map.plugin.command.exception.ConsoleMustProvideWorldException;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class Commands {
    public static final CloudKey<Pl3xMapPlugin> PLUGIN_INSTANCE_KEY = SimpleCloudKey.of("plugin-instance", TypeToken.get(Pl3xMapPlugin.class));

    private final PaperCommandManager<CommandSender> commandManager;

    public Commands(final Pl3xMapPlugin plugin) {
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
            final CloudBrigadierManager<?, ?> brigManager = this.commandManager.brigadierManager();
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
            new ShowCommand(plugin, this)
        ).forEach(Pl3xMapCommand::register);

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

    public void registerSubcommand(UnaryOperator<Command.Builder<CommandSender>> builderModifier) {
        this.commandManager.command(builderModifier.apply(this.rootBuilder()));
    }

    private Command.Builder<CommandSender> rootBuilder() {
        return this.commandManager.commandBuilder(Config.MAIN_COMMAND_LABEL, Config.MAIN_COMMAND_ALIASES.toArray(String[]::new))
            /* MinecraftHelp uses the MinecraftExtrasMetaKeys.DESCRIPTION meta, this is just so we give Bukkit a description
             * for our commands in the Bukkit and EssentialsX '/help' command */
            .meta(CommandMeta.DESCRIPTION, String.format("squaremap command. '/%s help'", Config.MAIN_COMMAND_LABEL));
    }

    public PaperCommandManager<CommandSender> commandManager() {
        return this.commandManager;
    }

}
