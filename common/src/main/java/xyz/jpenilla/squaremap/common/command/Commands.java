package xyz.jpenilla.squaremap.common.command;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.SimpleCloudKey;
import cloud.commandframework.meta.CommandMeta;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.leangen.geantyref.TypeToken;
import java.util.List;
import java.util.function.UnaryOperator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.AbstractPlayerManager;
import xyz.jpenilla.squaremap.common.ServerAccess;
import xyz.jpenilla.squaremap.common.WorldManager;
import xyz.jpenilla.squaremap.common.command.commands.CancelRenderCommand;
import xyz.jpenilla.squaremap.common.command.commands.ConfirmCommand;
import xyz.jpenilla.squaremap.common.command.commands.FullRenderCommand;
import xyz.jpenilla.squaremap.common.command.commands.HelpCommand;
import xyz.jpenilla.squaremap.common.command.commands.HideShowCommands;
import xyz.jpenilla.squaremap.common.command.commands.PauseRenderCommand;
import xyz.jpenilla.squaremap.common.command.commands.ProgressLoggingCommand;
import xyz.jpenilla.squaremap.common.command.commands.RadiusRenderCommand;
import xyz.jpenilla.squaremap.common.command.commands.ReloadCommand;
import xyz.jpenilla.squaremap.common.command.commands.ResetMapCommand;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.ConfigManager;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;

@DefaultQualifier(NonNull.class)
@Singleton
public final class Commands {
    public static final CloudKey<AbstractPlayerManager> PLAYER_MANAGER = createTypeKey(AbstractPlayerManager.class);
    public static final CloudKey<ServerAccess> SERVER_ACCESS = createTypeKey(ServerAccess.class);
    public static final CloudKey<RenderFactory> RENDER_FACTORY = createTypeKey(RenderFactory.class);
    public static final CloudKey<ConfigManager> CONFIG_MANAGER = createTypeKey(ConfigManager.class);
    public static final CloudKey<WorldManager> WORLD_MANAGER = createTypeKey(WorldManager.class);

    private final Injector injector;
    private final CommandManager<Commander> commandManager;

    @Inject
    private Commands(
        final Injector injector,
        final PlatformCommands platformCommands,
        final ExceptionHandler exceptionHandler,
        final AbstractPlayerManager playerManager,
        final ServerAccess serverAccess,
        final RenderFactory renderFactory,
        final ConfigManager configManager,
        final WorldManager worldManager
    ) {
        this.injector = injector;
        this.commandManager = platformCommands.createCommandManager();

        this.commandManager.registerCommandPreProcessor(preprocessContext -> {
            final CommandContext<Commander> commandContext = preprocessContext.getCommandContext();
            commandContext.store(PLAYER_MANAGER, playerManager);
            commandContext.store(SERVER_ACCESS, serverAccess);
            commandContext.store(RENDER_FACTORY, renderFactory);
            commandContext.store(CONFIG_MANAGER, configManager);
            commandContext.store(WORLD_MANAGER, worldManager);
        });

        exceptionHandler.registerExceptionHandlers(this.commandManager);
    }

    public void registerCommands() {
        final List<Class<? extends SquaremapCommand>> commands = List.of(
            HelpCommand.class,
            ReloadCommand.class,
            ConfirmCommand.class,
            FullRenderCommand.class,
            CancelRenderCommand.class,
            PauseRenderCommand.class,
            ResetMapCommand.class,
            ProgressLoggingCommand.class,
            RadiusRenderCommand.class,
            HideShowCommands.class
        );

        for (final Class<? extends SquaremapCommand> command : commands) {
            this.injector.getInstance(command).register();
        }
    }

    public void register(final Command.Builder<Commander> builder) {
        this.commandManager.command(builder);
    }

    public void registerSubcommand(UnaryOperator<Command.Builder<Commander>> builderModifier) {
        this.commandManager.command(builderModifier.apply(this.rootBuilder()));
    }

    public Command.Builder<Commander> rootBuilder() {
        return this.commandManager.commandBuilder(Config.MAIN_COMMAND_LABEL, Config.MAIN_COMMAND_ALIASES.toArray(String[]::new))
            /* MinecraftHelp uses the MinecraftExtrasMetaKeys.DESCRIPTION meta, this is just so we give Bukkit a description
             * for our commands in the Bukkit and EssentialsX '/help' command */
            .meta(CommandMeta.DESCRIPTION, String.format("squaremap command. '/%s help'", Config.MAIN_COMMAND_LABEL));
    }

    public CommandManager<Commander> commandManager() {
        return this.commandManager;
    }

    private static <T> CloudKey<T> createTypeKey(final Class<T> type) {
        return SimpleCloudKey.of("squaremap-" + type.getName(), TypeToken.get(type));
    }
}
