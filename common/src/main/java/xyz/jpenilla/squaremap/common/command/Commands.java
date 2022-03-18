package xyz.jpenilla.squaremap.common.command;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.SimpleCloudKey;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.leangen.geantyref.TypeToken;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.SquaremapPlatform;
import xyz.jpenilla.squaremap.common.command.commands.CancelRenderCommand;
import xyz.jpenilla.squaremap.common.command.commands.ConfirmCommand;
import xyz.jpenilla.squaremap.common.command.commands.FullRenderCommand;
import xyz.jpenilla.squaremap.common.command.commands.HelpCommand;
import xyz.jpenilla.squaremap.common.command.commands.PauseRenderCommand;
import xyz.jpenilla.squaremap.common.command.commands.ProgressLoggingCommand;
import xyz.jpenilla.squaremap.common.command.commands.ReloadCommand;
import xyz.jpenilla.squaremap.common.command.commands.ResetMapCommand;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.util.Components;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;

@DefaultQualifier(NonNull.class)
@Singleton
public final class Commands {
    public static final CloudKey<Injector> INJECTOR = SimpleCloudKey.of("squaremap-injector", TypeToken.get(Injector.class));
    public static final CloudKey<SquaremapCommon> COMMON = SimpleCloudKey.of("squaremap-common", TypeToken.get(SquaremapCommon.class));
    public static final CloudKey<SquaremapPlatform> PLATFORM = SimpleCloudKey.of("squaremap-platform", TypeToken.get(SquaremapPlatform.class));

    private final CommandManager<Commander> commandManager;

    @Inject
    private Commands(
        final Injector injector,
        final PlatformCommands platformCommands
    ) {
        this.commandManager = platformCommands.createCommandManager();

        this.commandManager.registerCommandPreProcessor(ctx -> {
            final CommandContext<Commander> commandContext = ctx.getCommandContext();
            commandContext.store(INJECTOR, injector);
            commandContext.store(COMMON, injector.getInstance(SquaremapCommon.class));
            commandContext.store(PLATFORM, injector.getInstance(SquaremapPlatform.class));
        });

        this.registerExceptionHandlers();

        List.of(
            new HelpCommand(this),
            new ReloadCommand(this),
            new ConfirmCommand(this),
            new FullRenderCommand(this),
            new CancelRenderCommand(this),
            new PauseRenderCommand(this),
            new ResetMapCommand(this),
            new ProgressLoggingCommand(this)
        ).forEach(SquaremapCommand::register);

        platformCommands.registerCommands(this);
    }

    private void registerExceptionHandlers() {
        new MinecraftExceptionHandler<Commander>()
            .withDefaultHandlers()
            .withDecorator(component -> text()
                .append(Components.miniMessage(Lang.COMMAND_PREFIX)
                    .hoverEvent(Components.miniMessage(Lang.CLICK_FOR_HELP))
                    .clickEvent(runCommand(String.format("/%s help", Config.MAIN_COMMAND_LABEL))))
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
            }

            minecraftExtrasDefaultHandler.accept(sender, exception);
        });
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
}
